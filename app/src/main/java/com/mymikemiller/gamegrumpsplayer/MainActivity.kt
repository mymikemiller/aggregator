package com.mymikemiller.gamegrumpsplayer

import android.content.Context
import android.content.Intent
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerView

import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.*
import com.mymikemiller.gamegrumpsplayer.util.VideoList
import com.mymikemiller.gamegrumpsplayer.yt.YouTubeAPI
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import com.mymikemiller.gamegrumpsplayer.util.WatchedMillis
import android.content.SharedPreferences
import com.mymikemiller.gamegrumpsplayer.util.SkippedGames


/**
 * A video player allowing users to watch Game Grumps episodes in chronological order while providing the ability to skip entire series.
 */
class MainActivity : YouTubeFailureRecoveryActivity(),
        YouTubePlayer.OnFullscreenListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private val CHANNEL_NAME = "gamegrumps"

    private val SKIP_GAMES = listOf<String>("Kirby")

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
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    private lateinit var mAdapter: RecyclerAdapter
    private lateinit var mUpButton: ImageView
    private lateinit var mDownButton: ImageView
    private lateinit var mTargetButton: ImageView
    private lateinit var mSearchEditText: EditText
    private lateinit var mExpandButton: ImageView
    private lateinit var mPreferencesButton: ImageView
    private var mInitialized: Boolean = false
    private lateinit var mAllUnskippedDetails: List<Detail>
    private lateinit var mSkipGameButton: Button

    var mAllDetailsIncludingSkipped = listOf<Detail>()

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
        mSearchEditText = findViewById(R.id.searchEditText)
        mExpandButton = findViewById(R.id.expand_button)
        mPreferencesButton = findViewById(R.id.preferences_button)
        mRecyclerView = findViewById(R.id.recyclerView)
        mLinearLayoutManager = LinearLayoutManager(this)
        mSkipGameButton = findViewById(R.id.skipGameButton)

        mSkipGameButton.setOnClickListener({
            run {
                val video = mCurrentlyPlayingVideoDetail
                if (video != null) {
                    addSkippedGame(video.game)
                }
            }
        })

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)

        mRecyclerView.setLayoutManager(mLinearLayoutManager)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        // Respond to keyboard up/down events
        val activityRootView = findViewById<LinearLayout>(R.id.layout)
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener({
            val heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight()

            if (heightDiff > 100) {
                // keyboard is up. Make the top half of screen go away to make room for the RecyclerView
                findViewById<LinearLayout>(R.id.playerContainer).visibility = View.GONE
                otherViews.visibility = View.GONE
                openPlaylist()
            } else {
                // keyboard is down. Bring the top half of the screen back.
                findViewById<LinearLayout>(R.id.playerContainer).visibility = View.VISIBLE
                otherViews.visibility = View.VISIBLE
            }
        })

        // Flip the Expand/retract button depending on the sliding layout state
        slidingLayout.addPanelSlideListener(object : SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelStateChanged(panel: View?, previousState: SlidingUpPanelLayout.PanelState?, newState: SlidingUpPanelLayout.PanelState?) {
                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    mExpandButton.scaleY = 1f
                } else {
                    mExpandButton.scaleY = -1f
                }
            }
            override fun onPanelSlide(panel: View, slideOffset: Float) {}
        })

        mExpandButton.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                if (slidingLayout.panelState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    openPlaylist()
                }else {
                    closePlaylist()
                }
            }
        })

        mPreferencesButton.setOnClickListener {
            // Display the fragment as the main content.
            val i = Intent(this, PreferencesActivity::class.java)
            startActivity(i)
        }

        val typeface: Typeface = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/gamegrumps.ttf")
        episodeTitle.setTypeface(typeface)

        mSearchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(text: Editable?) {
                filter(text.toString())
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

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
                    val startTimeMillis = WatchedMillis.getWatchedMillis(this, detail)
                    playVideo(detail, false, startTimeMillis)
                }
                // Hide the keyboard and collapse the slidingPanel if we click an item
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0)
                slidingLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
            }
        }

        setRecyclerViewScrollListener()
        //setRecyclerViewItemTouchListener() // Enable this to enable left/right swiping

        playerView.initialize(DeveloperKey.DEVELOPER_KEY, this)

        bar.getViewTreeObserver().addOnGlobalLayoutListener({
            if (bar.height > 0) {
                slidingLayout.panelHeight = bar.height
            }
        })

        // This happens once the details are fetched from YouTube. detailsList contains all the details, including those from the database.
        val detailsFetched: (List<Detail>) -> Unit = { allDetailsUnordered ->
            run {
                //TODO: we probably shouldn't be doing all this on the UI thread
                runOnUiThread {
                    // We need to cache the list of all videos so we can find
                    mAllDetailsIncludingSkipped = allDetailsUnordered

                    // Filter out the episodes the user has specified to skip
                    mAllUnskippedDetails = SkippedGames.filterOutSkipped(this, allDetailsUnordered)

                    // Now that we've got a list of details, we can
                    mAdapter = RecyclerAdapter(mAllUnskippedDetails, isSelected, onItemClick)
                    mRecyclerView.setAdapter(mAdapter)
                    mAdapter.notifyItemRangeChanged(0, mAllUnskippedDetails.size-1)
                    fetchVideosProgressSection.visibility = View.GONE

                    // Get the default first video (the channel's first video)
                    val firstDetail = mAllUnskippedDetails[0]

                    // Get the last video we were playing (which will be the next video in the playlist
                    // if it was queued at the end of the last watch session if it had time to try to load)
                    val sharedPref = getPreferences(Context.MODE_PRIVATE)
                    val videoIdToPlay = sharedPref.getString(getString(R.string.currentVideoId), firstDetail.videoId).toString()

                    var detailToPlay = VideoList.getDetailFromVideoId(this, videoIdToPlay)
                    if (detailToPlay == null) {
                        // If we couldn't find a video to play, play the chronologicallly irst video of the channel
                        detailToPlay = mAllUnskippedDetails[0]
                    }

                    val videoTimeToPlayMillis = WatchedMillis.getWatchedMillis(this, detailToPlay)
                    playVideo(detailToPlay, true, videoTimeToPlayMillis)

                    scrollToCurrentlyPlayingVideo()

                }

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
            openPlaylist()
            scrollToTop()
        }
        mDownButton.setOnClickListener {
            openPlaylist()
            scrollToBottom()
        }
        mTargetButton.setOnClickListener {
            openPlaylist()
            scrollToCurrentlyPlayingVideo()
        }

        fetchVideosProgressSection.visibility=View.VISIBLE
        YouTubeAPI.fetchChannelIdFromChannelName(CHANNEL_NAME, {channelId -> run {
            // Get the details ordered by date uploaded and force an upgrade if necessary, which will call the deleteCurrentVideoFromSharedPreferences call if
            // necessary.
            val details = VideoList.getAllDetailsFromDatabase(this,
                    getString(R.string.pref_playlistOrder_byDateUploaded),
                    deleteCurrentVideoFromSharedPreferences)

            val stopAtDetail = if (details.size > 0) details[details.size - 1] else null

            // Make sure the results come back sorted in the order we want
            val playlistOrder = getPreferredPlaylistOrder()

            VideoList.fetchAllDetailsByChannelId(this,
                    playlistOrder,
                    deleteCurrentVideoFromSharedPreferences, channelId,
                    stopAtDetail, setVideoFetchPercentageComplete, detailsFetched)

        }})
    }

    fun addSkippedGame(game: String) {
        // Get what would be our next video if that game were already skipped. getNextVideo does
        // that for us
        val nextVideo = getNextVideo()

        // nextVideo now refers to the first Detail that doesn't match the newly skipped game or any
        // skipped games or null if we're at the end of the playlist

        SkippedGames.addSkippedGame(this, game)

        mAllUnskippedDetails = SkippedGames.filterOutSkipped(this, mAllUnskippedDetails)
        mAdapter.details = mAllUnskippedDetails
        mAdapter.notifyDataSetChanged()
        if (!mAllUnskippedDetails.contains(mCurrentlyPlayingVideoDetail)) {
            // The user skipped the currently playing video. Play the next video in the adapter if there is one.
            if (nextVideo != null) {
                playVideo(nextVideo)
            }
        }
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
        if (key == getString(R.string.pref_playlistOrderKey)) {
            if (sp != null) {
                val playlistOrderKey = getString(R.string.pref_playlistOrderKey)
                val chronological = getString(R.string.pref_playlistOrder_byDateUploaded)
                val byGame = getString(R.string.pref_playlistOrder_byGame)
                val preference = sp.getString(playlistOrderKey, chronological)

                if (preference == byGame) {
                    mAdapter.details = getAllDetailsOrderedByGame()
                } else {
                    mAdapter.details = getAllDetailsOrderedByDateUploaded()
                }
                mAdapter.notifyDataSetChanged()
                scrollToCurrentlyPlayingVideo()
            }
        }
    }

    private fun getAllDetailsOrderedByDateUploaded(): List<Detail> {
        return VideoList.getAllDetailsFromDatabase(this,
                getString(R.string.pref_playlistOrder_byDateUploaded),
                deleteCurrentVideoFromSharedPreferences)
    }
    private fun getAllDetailsOrderedByGame(): List<Detail> {
        return VideoList.getAllDetailsFromDatabase(this,
                getString(R.string.pref_playlistOrder_byGame),
                deleteCurrentVideoFromSharedPreferences)
    }

    // If we press back when the sliding panel is visible, minimize it
    override fun onBackPressed() {
        if (slidingLayout.panelState != SlidingUpPanelLayout.PanelState.COLLAPSED) {
            slidingLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        } else {
            super.onBackPressed()
        }
    }

    private fun filter(query: String) {
        val lowerCaseQuery = query.toLowerCase()

        val filteredNames = mAllUnskippedDetails.filter {
            it.game.toLowerCase().contains(lowerCaseQuery) ||
            it.title.toLowerCase().contains(lowerCaseQuery) }

        mAdapter.details = filteredNames.toList()
        mAdapter.notifyDataSetChanged()
    }

    fun openPlaylist() {
        slidingLayout.panelState = SlidingUpPanelLayout.PanelState.EXPANDED
    }
    fun closePlaylist() {
        slidingLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
    }

    val deleteCurrentVideoFromSharedPreferences: () -> Unit = {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.remove(getString(R.string.currentVideoId))
        editor.apply()
    }

    override fun onInitializationSuccess(provider: YouTubePlayer.Provider, player: YouTubePlayer,
                                         wasRestored: Boolean) {
        this.player = player
        mInitialized = true
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
        if (mInitialized)
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
            // The start time is probably 0 because the video is probably not in the database yet,
            // but if it is, continue playing where we left off
            val startTimeMillis = WatchedMillis.getWatchedMillis(this, nextVideoDetail)
            // Play the next video, but don't scroll to it in case the user is looking somewhere else in the playlist
            playVideo(nextVideoDetail, false, startTimeMillis)
        }
    }

    private val recordCurrentTime: () -> Unit = {
        // The video was paused (or minimized or otherwise caused to pause playback)
        // Record the time we paused at so we can restore it when the app reloads
        val d: Detail? = mCurrentlyPlayingVideoDetail
        if (d != null) {
            WatchedMillis.addOrUpdateWatchedMillis(this, d, player.currentTimeMillis)
        }
    }

    private fun getNextVideo() : Detail? {
        val skippedGames = SkippedGames.getAllSkippedGames(this)
        var foundCurrentlyPlayingVideo = false
        val currentlyPlayingVideoDetail = mCurrentlyPlayingVideoDetail
        if (currentlyPlayingVideoDetail != null) {
            for (detail in mAllDetailsIncludingSkipped) {
                if (foundCurrentlyPlayingVideo) {
                    // Keep looping through mAllDetailsIncludingSkipped until we find one that wasn't skipped
                    if (!skippedGames.contains(detail.game)) {
                        return detail
                    }
                }
                if (detail == mCurrentlyPlayingVideoDetail) {
                    // We found the currently playing video. Now keep looping until we find a video not skipped
                    foundCurrentlyPlayingVideo = true
                }
            }
        }
        return null
    }
    private fun getPreferredPlaylistOrder(): String{
        // Get the preferred display order from Preferences
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val playlistOrderPref = sharedPref.getString(getString(R.string.pref_playlistOrderKey),
                getString(R.string.pref_playlistOrder_byDateUploaded))
        return playlistOrderPref
    }

    // Scroll the recyclerView to the playing video
    fun scrollToCurrentlyPlayingVideo() {
        val index = mAdapter.details.indexOf(mCurrentlyPlayingVideoDetail)
        runOnUiThread {
            // Scroll with an offset so that the selected video is one item down in the list
            mLinearLayoutManager.scrollToPositionWithOffset(index - 1, 0)
        }
    }

    fun scrollToTop() {
        val index = 0
        runOnUiThread {
            mLinearLayoutManager.scrollToPosition(index)
        }
    }
    fun scrollToBottom() {
        val index = mRecyclerView.adapter.itemCount - 1
        if (index > -1) {
            runOnUiThread {
                mLinearLayoutManager.scrollToPosition(index)
            }
        }
    }

    override val youTubePlayerProvider: YouTubePlayer.Provider
        get() = playerView

    override fun onFullscreen(isFullscreen: Boolean) {
        fullscreen = isFullscreen
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
