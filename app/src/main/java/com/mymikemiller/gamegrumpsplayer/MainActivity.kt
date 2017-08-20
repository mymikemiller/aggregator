package com.mymikemiller.gamegrumpsplayer

import android.content.Context
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerView

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.*
import com.mymikemiller.gamegrumpsplayer.util.VideoList
import com.mymikemiller.gamegrumpsplayer.yt.YouTubeAPI
import android.os.Handler
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import android.view.ViewTreeObserver



/**
 * A video player allowing users to watch Game Grumps episodes in chronological order while providing the ability to skip entire series.
 */
class MainActivity : YouTubeFailureRecoveryActivity(), YouTubePlayer.OnFullscreenListener {
    private val PLAYLIST_PEEK_Y = 200f
    private val CHANNEL_NAME = "gamegrumps"

    private lateinit var baseLayout: LinearLayout
    private lateinit var bar: LinearLayout
    private lateinit var slidingLayout: SlidingUpPanelLayout
    private lateinit var playerView: YouTubePlayerView
    private lateinit var player: YouTubePlayer
    private lateinit var otherViews: View
    private lateinit var fetchVideosProgressSection: LinearLayout
    private lateinit var fetchVideosProgresBar: ProgressBar
    private lateinit var episodeTitle: TextView
    private lateinit var episodeDescription: TextView
    private var fullscreen: Boolean = false
    private lateinit var playerStateChangeListener: MyPlayerStateChangeListener
    private lateinit var playbackEventListener: MyPlaybackEventListener
    private var mCurrentlyPlayingVideoDetail: Detail? = null
    val recordCurrentTimeHandler: Handler = Handler()
    var mDetailsList: MutableList<Detail> = mutableListOf()
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    private lateinit var mAdapter: RecyclerAdapter
    private var mItemHeight = 0
    private lateinit var mUpButton: ImageView
    private lateinit var mDownButton: ImageView
    private lateinit var mTargetButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        baseLayout = findViewById<LinearLayout>(R.id.layout)
        bar = findViewById<LinearLayout>(R.id.bar)
        slidingLayout = findViewById(R.id.sliding_layout)
        playerView = findViewById<YouTubePlayerView>(R.id.player)
        otherViews = findViewById(R.id.other_views)
        fetchVideosProgressSection = findViewById(R.id.fetchVideosProgressSection)
        fetchVideosProgresBar = findViewById(R.id.fetchVideosProgressBar)
        episodeTitle = findViewById<TextView>(R.id.episodeTitle)
        episodeDescription = findViewById<TextView>(R.id.episodeDescription)
        playerStateChangeListener = MyPlayerStateChangeListener(playNextVideo)
        playbackEventListener = MyPlaybackEventListener(recordCurrentTime, recordCurrentTimeHandler)
        mUpButton = findViewById(R.id.up_button)
        mDownButton = findViewById(R.id.down_button)
        mTargetButton = findViewById(R.id.target_button)

        val typeface: Typeface = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/gamegrumps.ttf")
        episodeTitle.setTypeface(typeface)

        mRecyclerView = findViewById<RecyclerView>(R.id.recyclerView) as RecyclerView
        mLinearLayoutManager = LinearLayoutManager(this)
        mRecyclerView.setLayoutManager(mLinearLayoutManager)

        // This callback will help the RecyclerView's DetailHolder know when to draw us as selected
        val isSelected: (Detail) -> Boolean = {detail ->
            run {
                detail == mCurrentlyPlayingVideoDetail
            }
        }
        // This callback will help the RecyclerView's DetailHolder know when to draw us as selected
        val onItemClick: (Detail) -> Unit = {detail ->
            run {
                if (detail != mCurrentlyPlayingVideoDetail) {
                    playVideo(detail, false)
                }
            }
        }

        mAdapter = RecyclerAdapter(mDetailsList, isSelected, onItemClick)
        mRecyclerView.setAdapter(mAdapter)

        setRecyclerViewScrollListener()
        //setRecyclerViewItemTouchListener() // Enable this to enable left/right swiping

        playerView.initialize(DeveloperKey.DEVELOPER_KEY, this)
        doLayout()

        // This isn't working for some reason...
        bar.getViewTreeObserver().addOnGlobalLayoutListener({
            bar.layoutParams = LinearLayout.LayoutParams(slidingLayout.width, bar.height)
        })

        val detailsFetched: (List<Detail>) -> Unit = { detailsList ->
            run {

                runOnUiThread {
                    mDetailsList.addAll(detailsList)
                    mAdapter.notifyItemRangeChanged(0, detailsList.size-1)
                    fetchVideosProgressSection.visibility = View.GONE
                }

                // Get the default first video (the channel's first video)
                val firstDetail = detailsList[0]

                // Get the last video we were playing (which will be the next video in the playlist
                // if it was queued at the end of the last watch session if it had time to try to load)
                val sharedPref = getPreferences(Context.MODE_PRIVATE)
                val videoIdToPlay = sharedPref.getString(getString(R.string.currentVideoId), firstDetail.videoId).toString()
                val videoTimeToPlayMillis = sharedPref.getInt(getString(R.string.currentVideoTimeMillis), 0)

                var detailToPlay = VideoList.getDetailFromVideoId(this, videoIdToPlay)
                if (detailToPlay == null) {
                    // If we couldn't find a video to play, play the first video of the channel
                    detailToPlay = VideoList.getAllDetailsFromDatabase(this, {})[0]
                }

                playVideo(detailToPlay, true, videoTimeToPlayMillis)

                scrollToCurrentlyPlayingVideo()
            }
        }
        val setVideoFetchPercentageComplete: (kotlin.Int, kotlin.Int) -> Unit = { totalVideos, currentVideoNumber ->
            run {
                val numDetailsInDatabase = VideoList.getNumDetailsInDatabase(this, {})
                fetchVideosProgresBar.max = (totalVideos - numDetailsInDatabase)
                fetchVideosProgresBar.setProgress(currentVideoNumber)
            }
        }

        mUpButton.setOnClickListener {
            scrollToTop()
        }
        mDownButton.setOnClickListener {
            scrollToBottom()
        }
        mTargetButton.setOnClickListener {
            scrollToCurrentlyPlayingVideo()
        }

        fetchVideosProgressSection.visibility=View.VISIBLE
        YouTubeAPI.fetchChannelIdFromChannelName(CHANNEL_NAME, {channelId -> run {
            // Force an upgrade if necessary, which will call the deleteSharedPreferences call if
            // necessary
            val existingDetails = VideoList.getAllDetailsFromDatabase(this, deleteSharedPreferences)

            val stopAtDetail = if (existingDetails.size > 0) existingDetails[existingDetails.size - 1] else null

            VideoList.fetchAllDetailsByChannelId(this, deleteSharedPreferences, channelId,
                    stopAtDetail, setVideoFetchPercentageComplete, detailsFetched)
        }})
    }

    val deleteSharedPreferences: () -> Unit = {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.remove(getString(R.string.currentVideoId))
        editor.apply()
    }

    override fun onInitializationSuccess(provider: YouTubePlayer.Provider, player: YouTubePlayer,
                                         wasRestored: Boolean) {
        this.player = player
        player.setPlayerStateChangeListener(playerStateChangeListener)
        player.setPlaybackEventListener(playbackEventListener)

        // Specify that we want to handle fullscreen behavior ourselves.
        player.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_CUSTOM_LAYOUT)
        player.setOnFullscreenListener(this)

        var controlFlags = player.fullscreenControlFlags
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        controlFlags = controlFlags or YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE
        player.fullscreenControlFlags = controlFlags
    }

    override fun onPause() {
        super.onPause()
        recordCurrentTime()
    }

    private class MyPlayerStateChangeListener(val videoEndCallback: () -> Unit) : YouTubePlayer.PlayerStateChangeListener {
        override fun onAdStarted() {
        }

        override fun onLoading() {
        }

        override fun onVideoStarted() {
        }

        override fun onLoaded(p0: String?) {
        }

        override fun onError(p0: YouTubePlayer.ErrorReason?) {
        }

        override fun onVideoEnded() {
            videoEndCallback()
        }
    }

    private class MyPlaybackEventListener(val recordCurrentTimeCallback: () -> Unit, val recordCurrentTimeHandler: Handler) : YouTubePlayer.PlaybackEventListener {
        override fun onPlaying() {
            // This runnable happens every 5 seconds and records the current play time to
            // SharedPreferences, until recordCurrentTimeHandler.removeCallbacksAndMessages(null)
            // is called
            val backupCurrentTime = object: Runnable {
                override fun run() {
                    recordCurrentTimeCallback()
                    recordCurrentTimeHandler.postDelayed(this, 5000)
                }
            }
            recordCurrentTimeHandler.post(backupCurrentTime)
        }

        override fun onBuffering(isBuffering: Boolean) {
        }

        override fun onStopped() {
            // Prevent the current time caching from happening every 5 seconds when we're paused
            recordCurrentTimeHandler.removeCallbacksAndMessages(null)
        }

        override fun onPaused() {
            recordCurrentTimeCallback()

            // Prevent the current time caching from happening every 5 seconds when we're paused
            recordCurrentTimeHandler.removeCallbacksAndMessages(null)
        }

        override fun onSeekTo(endPositionMillis: Int) {
        }
    }


    private val playNextVideo: () -> Unit = {
        // Cue up the next video
        val nextVideoDetail: Detail? = getNextVideo()
        if (nextVideoDetail != null) {
            episodeTitle.setText(nextVideoDetail.title)
            episodeDescription.setText(nextVideoDetail.description)
            // Play the next video, but don't scroll to it in case the user is looking somewhere else in the playlist
            playVideo(nextVideoDetail)
        }
    }

    private val recordCurrentTime: () -> Unit = {
        // The video was paused (or minimized or otherwise caused to pause playback)
        // Record the time we paused at so we can restore it when the app reloads
        val preferences = getPreferences(Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putInt(getString(R.string.currentVideoTimeMillis), player.currentTimeMillis)
        editor.commit()
    }

    private fun getNextVideo() : Detail? {
        val details = VideoList.getAllDetailsFromDatabase(this, deleteSharedPreferences)
        var found = false
        for(detail in details) {
            if (found) {
                return detail
            }
            if (mCurrentlyPlayingVideoDetail != null && detail == mCurrentlyPlayingVideoDetail) {
                found = true
            }
        }
        return null
    }

    // Scroll the recyclerView to the playing video
    fun scrollToCurrentlyPlayingVideo() {
        val index = mDetailsList.indexOf(mCurrentlyPlayingVideoDetail)
        runOnUiThread {
            // Scroll with an offset so that the selected video is one item down in the list
            // This is a dumb way to find that offset, but it works.
            if (mItemHeight == 0) {
                for (i in 0..mDetailsList.size) {
                    val detailHolder = mRecyclerView.findViewHolderForLayoutPosition(i)
                    if (detailHolder != null) {
                        mItemHeight = detailHolder.itemView.height
                        break
                    }
                }
            }

            mLinearLayoutManager.scrollToPositionWithOffset(index, mItemHeight)
        }
    }

    fun scrollToTop() {
        val index = 0
        runOnUiThread {
            mLinearLayoutManager.scrollToPositionWithOffset(index, 0)
        }
    }
    fun scrollToBottom() {
        val index = mDetailsList.indexOf(mDetailsList[mDetailsList.size - 1])
        runOnUiThread {
            mLinearLayoutManager.scrollToPositionWithOffset(index, mItemHeight)
        }
    }

    override val youTubePlayerProvider: YouTubePlayer.Provider
        get() = playerView

    private fun doLayout() {
        if (fullscreen) {
            // When in fullscreen, the visibility of all other views than the player should be set to
            // GONE and the player should be laid out across the whole screen.
//            otherViews.visibility = View.GONE
        } else {
            // vertically stacked boxes in portrait, horizontally stacked in landscape.
//            otherViews.visibility = View.VISIBLE
        }
    }

    override fun onFullscreen(isFullscreen: Boolean) {
        fullscreen = isFullscreen
        doLayout()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        doLayout()
    }

    fun playVideo(detail: Detail?, centerPlaylistItem: Boolean = true, startTimeMillis: Int = 0) {
        if (detail != null) {
            mCurrentlyPlayingVideoDetail = detail

            runOnUiThread {
                episodeTitle.setText(detail.title)
                episodeDescription.setText(detail.description)
                player.loadVideo(detail.videoId, startTimeMillis)

                // Refresh the RecyclerAdapter to get the currently playing highlight right
                mRecyclerView.adapter.notifyDataSetChanged()
            }

            // Save the Detail to SharedPreference so we can start there next time
            val preferences = getPreferences(Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString(getString(R.string.currentVideoId), detail.videoId)
            editor.apply()
            if (centerPlaylistItem)
                scrollToCurrentlyPlayingVideo()
        }
    }

    private fun setRecyclerViewScrollListener() {
        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }
        })
    }

    private fun setRecyclerViewItemTouchListener()
    {
        val itemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, viewHolder1: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                // This shouldn't be called because we disabled the call to setRecyclerViewItemTouchListener
//                val position = viewHolder.adapterPosition
//                mDetailsList.removeAt(position)
//                mRecyclerView.adapter.notifyItemRemoved(position)
            }
        }

        //4
        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchHelper.attachToRecyclerView(mRecyclerView)
    }

    private fun getLastVisibleItemPosition(): Int {
        return mLinearLayoutManager.findLastVisibleItemPosition()
    }
}
