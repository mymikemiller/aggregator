package com.mymikemiller.chronoplayer

import android.app.Activity
import android.content.*
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerView

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.*
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.ViewPager
import android.content.Intent
import android.util.Log
import com.mymikemiller.chronoplayer.util.*


/**
 * A video player allowing users to watch YouTube episodes in chronological order while providing the ability to skip videos.
 */
class MainActivity : YouTubeFailureRecoveryActivity(),
        YouTubePlayer.OnFullscreenListener {

    val WATCH_HISTORY_REQUEST = 1  // The request code from the WatchHistoryActivity activity
    val CHANNEL_SELECT_REQUEST = 2  // The request code from the ChannelSelectActivity activity

    //region [Variable definitions]
    private lateinit var mChannel: Channel
    private lateinit var baseLayout: LinearLayout
    private lateinit var bar: LinearLayout
    private lateinit var slidingLayout: SlidingUpPanelLayout
    private lateinit var playerView: YouTubePlayerView
    private lateinit var player: YouTubePlayer
    private lateinit var otherViews: View
    private lateinit var fetchVideosProgressSection: LinearLayout
    private lateinit var fetchVideosProgresBar: ProgressBar
    private var fullscreen: Boolean = false
    private lateinit var playerStateChangeListener: MyPlayerStateChangeListener
    private lateinit var playbackEventListener: MyPlaybackEventListener
    private var mCurrentlyPlayingVideoDetail: Detail? = null
    val recordCurrentTimeHandler: Handler = Handler()
    private lateinit var mPlaylist: RecyclerView
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    private var mAdapter: RecyclerAdapter = RecyclerAdapter(this, listOf(), {true}, {true}, null)
    private lateinit var mUpButton: ImageView
    private lateinit var mDownButton: ImageView
    private lateinit var mTargetButton: ImageView
    private lateinit var mSearchEditText: EditText
    private lateinit var mExpandButton: ImageView
    private lateinit var mPreferencesButton: ImageView
    private var mPlayerInitialized: Boolean = false
    private var mAdapterInitialized: Boolean = false
    private lateinit var mBroadcastReceiver: BroadcastReceiver
    private lateinit var mEpisodePager: ViewPager
    private lateinit var mEpisodeViewPagerAdapter: EpisodePagerAdapter

    // These collections include the skipped cideos
    var mDetailsByDateIncludingSkipped = listOf<Detail>()

    // These collections have the skipped videos filtered out
    var mDetailsByDate = listOf<Detail>()

    // endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //region [Variable initialization]
        baseLayout = findViewById<LinearLayout>(R.id.layout)
        bar = findViewById<LinearLayout>(R.id.bar)
        slidingLayout = findViewById(R.id.sliding_layout)
        playerView = findViewById<YouTubePlayerView>(R.id.player)
        otherViews = findViewById(R.id.other_views)
        fetchVideosProgressSection = findViewById(R.id.fetchVideosProgressSection)
        fetchVideosProgresBar = findViewById(R.id.fetchVideosProgressBar)
        playerStateChangeListener = MyPlayerStateChangeListener(playNextVideo)
        playbackEventListener = MyPlaybackEventListener(recordCurrentTime, recordCurrentTimeHandler)
        mUpButton = findViewById(R.id.up_button)
        mDownButton = findViewById(R.id.down_button)
        mTargetButton = findViewById(R.id.target_button)
        mSearchEditText = findViewById(R.id.searchEditText)
        mExpandButton = findViewById(R.id.expand_button)
        mPreferencesButton = findViewById(R.id.preferences_button)
        mPlaylist = findViewById(R.id.playlist)
        mLinearLayoutManager = LinearLayoutManager(this)
        mEpisodePager = findViewById(R.id.episodeViewPager)
        // endregion

        setUp(intent)
    }

    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)
        if (newIntent != null) {
            setUp(newIntent)
        }
    }

    fun setUp(theIntent: Intent) {
        mChannel = theIntent.getSerializableExtra("channel") as Channel

        // Save the launch channel to sharedPreferences so we start there next time
        saveLaunchChannel(mChannel)

        setUpYouTubeFetch()
        setUpPlayer()
        setUpEpisodePager()
        setUpPlaylist()
        setUpSearch()
        setUpPreferences()
    }

    private fun saveLaunchChannel(channel: Channel) {
        // Store the channel in shared preferences so we can go right to MainAcivity when we start up next time
        val preferences = getSharedPreferences(getString(R.string.sharedPrefsName), Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString(getString(R.string.launchChannel), channel.channelId)
        editor.apply()

        // insert the Channel into the Channels database (or do nothing if it's already there).
        // We'll need it to launch MainActivity right away the next time LaunchActivity loads
        Channels.addChannel(applicationContext, channel)
    }

    private fun setUpYouTubeFetch() {
        // Get the details ordered by date uploaded and force an upgrade if necessary,s
        val detailsFromDbByDate = PlaylistManipulator.orderByDate(VideoList.getAllDetailsFromDb(this,
                mChannel))

        // This won't work until we've initialized these lists
        val stopAtDetail = if (detailsFromDbByDate.size > 0) detailsFromDbByDate[detailsFromDbByDate.size - 1] else null

        // Set up what happens when an playlist item is clicked
        val onItemClick: (Detail) -> Unit = {detail ->
            run {
                if (detail != mCurrentlyPlayingVideoDetail) {
                    playVideo(detail, false)
                }
                // Hide the keyboard and collapse the slidingPanel if we click an item
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0)
                slidingLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
            }
        }

        // This happens once the details are fetched from YouTube. detailsList contains all the
        // details, including those from the database, skipped or not.
        val detailsFetched: (List<Detail>) -> Unit = { allDetailsUnordered ->
            run {
                //TODO: we probably shouldn't be doing all this on the UI thread
                runOnUiThread {
                    val orderedByDateIncludingSkipped = PlaylistManipulator.orderByDate(allDetailsUnordered)

                    mDetailsByDateIncludingSkipped = orderedByDateIncludingSkipped

                    mDetailsByDate = SkippedVideos.filterOutSkipped(this, mChannel, mDetailsByDateIncludingSkipped)

                    // Now that we've got a list of details, we can prepare the RecyclerView
                    mAdapter = RecyclerAdapter(this, mDetailsByDate, isSelected, onItemClick, skipVideo)
                    mEpisodeViewPagerAdapter = EpisodePagerAdapter(this, mDetailsByDate)
                    mEpisodePager.setAdapter(mEpisodeViewPagerAdapter)

                    mAdapterInitialized = true
                    mPlaylist.setAdapter(mAdapter)
                    mAdapter.notifyItemRangeChanged(0, mDetailsByDate.size-1)
                    fetchVideosProgressSection.visibility = View.GONE

                    // If the channel has no videos, don't play anything.
                    if (mDetailsByDate.size > 0) {

                        // Get the default first video (the channel's first video)
                        val firstVideoId = mDetailsByDate[0].videoId

                        // Get the last video we were playing (which will be the next video in the playlist
                        // if it was queued at the end of the last watch session if it had time to try to load)
                        var videoIdToPlay = LastPlayedVideo.getLastPlayedVideoId(this, mChannel)
                        if (videoIdToPlay == "") {
                            videoIdToPlay = firstVideoId
                        }

                        //val videoIdToPlay = sharedPref.getString(getString(R.string.currentVideoId), firstDetail.videoId).toString()

                        var detailToPlay = VideoList.getDetail(this, mChannel, videoIdToPlay)
                        if (detailToPlay == null) {
                            // If we couldn't find a video to play, play the chronologically first video of the channel
                            detailToPlay = VideoList.getDetail(this, mChannel, videoIdToPlay)
                        }

                        playVideo(detailToPlay, true)

                        scrollToCurrentlyPlayingVideo()
                    }
                }
            }
        }

        VideoList.fetchAllDetails(this,
                mChannel,
                stopAtDetail, setVideoFetchPercentageComplete, detailsFetched)
    }

    val isSelected: (Detail) -> Boolean = {detail ->
        run {
            detail == mCurrentlyPlayingVideoDetail
        }
    }

    val skipVideo: (detail: Detail) -> Unit = {detail ->
        run {
            addSkippedVideo(detail)
            notifyPlaylistItemsRemoved(detail)
        }
    }

    private fun notifyPlaylistItemsRemoved(detail: Detail) {
        for (i in mAdapter.details.indices) {
            val iDetail = mAdapter.details[i]
            if (detail == iDetail) {
                // Make the RecyclerView items scroll up to fill in the space
                mPlaylist.adapter.notifyItemRemoved(i)
            }
        }
    }


    private fun setUpPlayer() {
        playerView.initialize(DeveloperKey.DEVELOPER_KEY, this)
    }
    private fun setUpEpisodePager() {
        mEpisodePager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                if (state == 0) { // finished scrolling
                    val detail = mEpisodeViewPagerAdapter.details[mEpisodePager.currentItem]
                    playVideo(detail)
                }
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {}
        })
    }
    private fun setUpPlaylist() {
        mPlaylist.setLayoutManager(mLinearLayoutManager)

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

        bar.getViewTreeObserver().addOnGlobalLayoutListener({
            if (bar.height > 0) {
                slidingLayout.panelHeight = bar.height
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
    }

    private fun setUpSearch() {
        mSearchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(text: Editable?) {
                filter(text.toString())
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })
    }
    private fun setUpPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        mPreferencesButton.setOnClickListener {
            showPreferencesActivity()
        }
        mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                val currentlyPlaying = mCurrentlyPlayingVideoDetail
                if (currentlyPlaying != null) {

                    when (intent?.action) {
                        PreferencesActivity.UNSKIP_ALL -> unSkipAllVideos(currentlyPlaying.channel)
                        PreferencesActivity.WATCH_HISTORY -> showWatchHistoryActivity(currentlyPlaying.channel)
                        PreferencesActivity.CHANNEL_SELECT -> showChannelSelectActivity(currentlyPlaying.channel)
                    }
                }
            }
        }

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mBroadcastReceiver, IntentFilter(PreferencesActivity.UNSKIP_ALL))
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mBroadcastReceiver, IntentFilter(PreferencesActivity.WATCH_HISTORY))
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mBroadcastReceiver, IntentFilter(PreferencesActivity.CHANNEL_SELECT))
    }

    fun showPreferencesActivity() {
        val i = Intent(this, PreferencesActivity::class.java)
        startActivity(i)
    }

    val setVideoFetchPercentageComplete: (kotlin.Int, kotlin.Int) -> Unit = { totalVideos, currentVideoNumber ->
        run {
            val numDetailsInDatabase = VideoList.getNumDetailsInDb(this, mChannel)
            fetchVideosProgresBar.max = (totalVideos - numDetailsInDatabase)
            fetchVideosProgresBar.setProgress(currentVideoNumber)
        }
    }

    //region [initialization]
    override fun onInitializationSuccess(provider: YouTubePlayer.Provider, player: YouTubePlayer,
                                         wasRestored: Boolean) {
        this.player = player
        mPlayerInitialized = true
        player.setPlayerStateChangeListener(playerStateChangeListener)
        player.setPlaybackEventListener(playbackEventListener)

        // Specify that we want to handle fullscreen behavior ourselves.
        player.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_CUSTOM_LAYOUT)
        player.setOnFullscreenListener(this)

        var controlFlags = player.fullscreenControlFlags
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        controlFlags = controlFlags or YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE
        player.fullscreenControlFlags = controlFlags

        // We may get here after we failed to play the video if we tried playing it before
        // initializing the player. Now that it's initialized we can play the video.
        playVideo(mCurrentlyPlayingVideoDetail)
    }
    //endregion

    //region [lifecycle]
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mBroadcastReceiver)
    }

    override fun onPause() {
        super.onPause()
        if (mPlayerInitialized)
            recordCurrentTime()
    }
    //endregion

    //region [layout]
    // If we press back when the sliding panel is visible, minimize it
    override fun onBackPressed() {
        if (slidingLayout.panelState != SlidingUpPanelLayout.PanelState.COLLAPSED) {
            slidingLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        } else {
            super.onBackPressed()
        }
    }
    //endregion

    //region [playlist functions]
    private fun refreshPlaylist() {
        mAdapter.details = mDetailsByDate
        mAdapter.notifyDataSetChanged()
        scrollToCurrentlyPlayingVideo()
    }
    fun openPlaylist() {
        slidingLayout.panelState = SlidingUpPanelLayout.PanelState.EXPANDED
    }
    fun closePlaylist() {
        slidingLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
    }
    //endregion

    //region [handle skipped videos]
    fun addSkippedVideo(detail: Detail) {

        SkippedVideos.addSkippedVideo(this, detail)

        // Get what would be our next video if that video were already skipped. getNextVideo does
        // that for us, even if we've skipped the currently playing video.
        val nextVideo = getNextVideo()

        // Update our cached lists
        mDetailsByDate = SkippedVideos.filterOutSkipped(this, detail.channel, mDetailsByDate)

        // Update the adapter
        mAdapter.details = mDetailsByDate
        mAdapter.notifyDataSetChanged()
        mEpisodeViewPagerAdapter.details = mAdapter.details
        mEpisodeViewPagerAdapter.notifyDataSetChanged()


        if (!mDetailsByDate.contains(mCurrentlyPlayingVideoDetail)) {
            // The user skipped the currently playing video. Play the next video in the adapter if there is one.
            if (nextVideo != null) {
                playVideo(nextVideo)
            }
        }
    }
    fun showWatchHistoryActivity(channel: Channel) {
        val watchHistoryIntent = Intent(this, WatchHistoryActivity::class.java)
        watchHistoryIntent.putExtra("channel", channel)
        watchHistoryIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivityForResult(watchHistoryIntent, WATCH_HISTORY_REQUEST)
    }

    fun showChannelSelectActivity(channel: Channel) {
        val launchActivityIntent = Intent(this, LaunchActivity::class.java)

        // Specify to the LaunchActivity that we came from settings so it doesn't automatically
        // load the channel we're currently on
        launchActivityIntent.putExtra(getString(R.string.launchedFromSettings), true)
        launchActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(launchActivityIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        // Check which request we're responding to
        if (requestCode == WATCH_HISTORY_REQUEST) {
            if (resultCode == Activity.RESULT_OK ) {
                // We clicked a video.  Find it and play it.
                val videoId = data.getStringExtra(WatchHistoryActivity.WATCH_HISTORY_DETAIL)
                val detailToPlay = mDetailsByDateIncludingSkipped.find { it.videoId == videoId }
                playVideo(detailToPlay)

                // Collapse the playlist because the user had to open it to get to the preferences
                slidingLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // Launch the preferences pane so it looks like we went back to it from the warch history
                showPreferencesActivity()
            }
        }
    }

    fun unSkipAllVideos(channel: Channel) {
        SkippedVideos.unSkipAllVideos(this, channel)

        // Update our cached list
        mDetailsByDate = mDetailsByDateIncludingSkipped

        if (mAdapterInitialized)
            refreshPlaylist()
    }
    //endregion

    // Filter the list of details by the search term in the title
    private fun filter(query: String) {
        var lowerCaseQuery = query.toLowerCase()

        // Ignore a space at the end
        if(lowerCaseQuery.endsWith(" ")) {
            lowerCaseQuery = lowerCaseQuery.substring(0, lowerCaseQuery.length - 1)
        }

        val filteredNames = mDetailsByDate.filter {
            it.title.toLowerCase().contains(lowerCaseQuery) }

        mAdapter.details = filteredNames.toList()
        mAdapter.notifyDataSetChanged()
    }


    private class MyPlayerStateChangeListener(val videoEndCallback: () -> Unit) : YouTubePlayer.PlayerStateChangeListener {
        override fun onVideoEnded() {
            // Play the next video by calling the callback
            videoEndCallback()
        }
        override fun onAdStarted() {}
        override fun onLoading() {}
        override fun onVideoStarted() {}
        override fun onLoaded(p0: String?) {}
        override fun onError(p0: YouTubePlayer.ErrorReason?) {}
    }

    private class MyPlaybackEventListener(
            val recordCurrentTimeCallback: () -> Unit,
            val recordCurrentTimeHandler: Handler) : YouTubePlayer.PlaybackEventListener {

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

        override fun onStopped() {
            // Prevent the current time caching from happening every 5 seconds when we're paused
            recordCurrentTimeHandler.removeCallbacksAndMessages(null)
        }

        override fun onPaused() {
            recordCurrentTimeCallback()

            // Prevent the current time caching from happening every 5 seconds when we're paused
            recordCurrentTimeHandler.removeCallbacksAndMessages(null)
        }

        override fun onSeekTo(endPositionMillis: Int) {}
        override fun onBuffering(isBuffering: Boolean) {}
    }


    private val playNextVideo: () -> Unit = {
        // First reset the watched time to 0 for the current video
        // so we start over at the beginning when playing this video next
        val currentlyPlaying = mCurrentlyPlayingVideoDetail
        if (currentlyPlaying != null) {
            WatchedMillis.addOrUpdateWatchedMillis(this, currentlyPlaying, 0)
        }

        // Cue up the next video
        val nextVideoDetail: Detail? = getNextVideo()
        if (nextVideoDetail != null) {
            // Play the next video, but don't scroll to it in case the user is looking somewhere else in the playlist
            playVideo(nextVideoDetail, true)
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
        // If we're currently playing a video, start the search. Otherwise return null because we
        // must be at the end of the playlist.
        val currentlyPlayingVideoDetail = mCurrentlyPlayingVideoDetail
        if (currentlyPlayingVideoDetail != null) {

            // We need to know the list of skipped videos so we make sure we don't play one that is
            // meant to be skipped. But when finding our current place in the playlist, we need to work
            // with all videos including the skipped ones in case the user just specified to skip a video
            // they're currently playing
            val skippedVideos = SkippedVideos.getAllSkippedVideos(this, currentlyPlayingVideoDetail.channel)

            // This will be true once we found the current video. Once we have that, we keep looping
            // through all the videos until we find one that isn't specified as skipped
            var foundCurrentlyPlayingVideo = false

            // As explained above, we need to search through all the videos, including skipped ones,
            // in order to find the currently playing video.
            for (detail in mDetailsByDateIncludingSkipped) {
                if (foundCurrentlyPlayingVideo) {
                    // Once we've found the current video, continue looping through
                    // all videos until we find one that wasn't skipped
                    if (!skippedVideos.contains(detail.videoId)) {
                        return detail
                    }
                }
                if (detail == mCurrentlyPlayingVideoDetail) {
                    // We found the currently playing video. Now keep looping until we find a video
                    // not skipped
                    foundCurrentlyPlayingVideo = true
                }
            }
        }
        return null
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
        val index = if (mPlaylist.adapter == null) -1 else mPlaylist.adapter.itemCount - 1
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

    fun playVideo(detail: Detail?, centerPlaylistItem: Boolean = true) {
        if (detail != null) {
            mCurrentlyPlayingVideoDetail = detail


            val startTimeMillis = WatchedMillis.getWatchedMillis(this, detail)

            runOnUiThread {
                // Find the right detail to switch the episode viewpager to
                mEpisodePager.currentItem = mEpisodeViewPagerAdapter.details.indexOf(detail)

                if (mPlayerInitialized)
                    player.loadVideo(detail.videoId, startTimeMillis)

                // Refresh the RecyclerAdapter to get the currently playing highlight right
                mPlaylist.adapter.notifyDataSetChanged()
            }

            // TODO: This should be in a database rather than SharedPreferences because now we have to track every one in the database
            // Save the Detail to the database so we can start there next time
            // this database uses ChannelId for the key and VideoId for the value
            LastPlayedVideo.addOrUpdateLastPlayedVideo(this, mChannel, detail)


            if (centerPlaylistItem)
                scrollToCurrentlyPlayingVideo()

            // Save the detail to Watch History
            WatchHistory.addDetail(this, detail)
        }
    }
}