package com.mymikemiller.gamegrumpsplayer

import android.content.Context
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerView

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.mymikemiller.gamegrumpsplayer.util.VideoList
import com.mymikemiller.gamegrumpsplayer.yt.YouTubeAPI

/**
 * A video player allowing users to watch Game Grumps episodes in chronological order while providing the ability to skip entire series.
 */
class MainActivity : YouTubeFailureRecoveryActivity(), YouTubePlayer.OnFullscreenListener {
    private lateinit var baseLayout: LinearLayout
    private lateinit var playerView: YouTubePlayerView
    private lateinit var player: YouTubePlayer
    private lateinit var otherViews: View
    private lateinit var fetchVideosProgressSection: LinearLayout
    private lateinit var fetchVideosProgresBar: ProgressBar
    private lateinit var episodeTitle: TextView
    private lateinit var episodeDescription: TextView
    private var fullscreen: Boolean = false
    private lateinit var playerStateChangeListener: MyPlayerStateChangeListener
    private var playingVideoDetail: Detail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        baseLayout = findViewById<LinearLayout>(R.id.layout)
        playerView = findViewById<YouTubePlayerView>(R.id.player)
        otherViews = findViewById(R.id.other_views)
        fetchVideosProgressSection = findViewById(R.id.fetchVideosProgressSection)
        fetchVideosProgresBar = findViewById(R.id.fetchVideosProgressBar)
        episodeTitle = findViewById<TextView>(R.id.episodeTitle)
        episodeDescription = findViewById<TextView>(R.id.episodeDescription)
        playerStateChangeListener = MyPlayerStateChangeListener(playNextVideo)

        playerView.initialize(DeveloperKey.DEVELOPER_KEY, this)
        doLayout()

        val startPlayingNext: (List<Detail>, String) -> Unit = { detailsList, finalPageToken ->
            run {
                runOnUiThread {
                    fetchVideosProgressSection.visibility = View.GONE
                }

                // Get the default first video (the channel's first video)
                val firstDetail = detailsList[0]

                // Get the last video we were playing (which will be the next video in the playlist if it was queued at the end of the last watch session if it had time to try to load)
                val sharedPref = getPreferences(Context.MODE_PRIVATE)
                val videoIdToPlay = sharedPref.getString(getString(R.string.currentVideoId), firstDetail.videoId).toString()

                var detailToPlay = VideoList.getDetailFromVideoId(this, videoIdToPlay)
                if (detailToPlay == null) {
                    // If we couldn't find a video to play, play the first video of the channel
                    detailToPlay = VideoList.getAllDetailsFromDatabase(this)[0]
                }

                playVideo(detailToPlay)

                // save the finalPageToken in SharedPreferences so we can start at that page next time we fetch the videos from YouTube
                val preferences = getPreferences(Context.MODE_PRIVATE)
                val editor = preferences.edit()
                editor.putString(getString(R.string.finalPageToken), finalPageToken)
                editor.commit()
            }
        }
        val setVideoFetchPercentageComplete: (kotlin.Int, kotlin.Int) -> Unit = { totalVideos, currentVideoNumber ->
            run {
                val numDetailsInDatabase = VideoList.getNumDetailsInDatabase(this, {})
                fetchVideosProgresBar.max = (totalVideos - numDetailsInDatabase)
                fetchVideosProgresBar.setProgress(currentVideoNumber)
            }
        }
        val deleteSharedPreference: () -> Unit = {
            val sharedPref = getPreferences(Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.remove(getString(R.string.finalPageToken))
            editor.remove(getString(R.string.currentVideoId))
            editor.commit()
            println("deleted")
        }
        // channelId for gamegrumps: UU9CuvdOVfMPvKCiwdGKL3cQ
        fetchVideosProgressSection.visibility=View.VISIBLE
        YouTubeAPI.fetchChannelIdFromChannelName("gamegrumps", {channelId -> run {
            // Force an upgrade if necessary
            VideoList.getNumDetailsInDatabase(this, deleteSharedPreference)

            val sharedPref = getPreferences(Context.MODE_PRIVATE)
            val previousFinalPageToken = sharedPref.getString(getString(R.string.finalPageToken), "").toString()

            VideoList.fetchAllDetailsByChannelId(this, deleteSharedPreference, channelId,
                    previousFinalPageToken, setVideoFetchPercentageComplete, startPlayingNext)
        }})
    }

    override fun onInitializationSuccess(provider: YouTubePlayer.Provider, player: YouTubePlayer,
                                         wasRestored: Boolean) {
        this.player = player
        player.setPlayerStateChangeListener(playerStateChangeListener)

        // Specify that we want to handle fullscreen behavior ourselves.
        player.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_CUSTOM_LAYOUT)
        player.setOnFullscreenListener(this)

        var controlFlags = player.fullscreenControlFlags
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        controlFlags = controlFlags or YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE
        player.fullscreenControlFlags = controlFlags
    }
    private class MyPlayerStateChangeListener(val videoEndCallback: () -> Unit) : YouTubePlayer.PlayerStateChangeListener {
        override fun onAdStarted() {
            println("Ad Started")
        }

        override fun onLoading() {
        }

        override fun onVideoStarted() {
            println("started")
            
        }

        override fun onLoaded(p0: String?) {
        }

        override fun onError(p0: YouTubePlayer.ErrorReason?) {
            println("Error")
        }

        override fun onVideoEnded() {
            println("ended")
            videoEndCallback()
        }
    }

    private val playNextVideo: () -> Unit = {
        // Cue up the next video
        val nextVideoDetail: Detail? = getNextVideo()
        if (nextVideoDetail != null) {
            episodeTitle.setText(nextVideoDetail.title)
            episodeDescription.setText(nextVideoDetail.description)
            playVideo(nextVideoDetail)
        }
    }

    private fun getNextVideo() : Detail? {
        val details = VideoList.getAllDetailsFromDatabase(this)
        var found = false
        for(detail in details) {
            if (found) {
                return detail
            }
            if (playingVideoDetail != null && detail == playingVideoDetail) {
                found = true
            }
        }
        return null
    }

    override val youTubePlayerProvider: YouTubePlayer.Provider
        get() = playerView

    private fun doLayout() {
        if (fullscreen) {
            // When in fullscreen, the visibility of all other views than the player should be set to
            // GONE and the player should be laid out across the whole screen.
            otherViews.visibility = View.GONE
        } else {
            // vertically stacked boxes in portrait, horizontally stacked in landscape.
            otherViews.visibility = View.VISIBLE
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


    fun playVideo(detail: Detail?) {
        if (detail != null) {
            runOnUiThread {
                episodeTitle.setText(detail.title)
                episodeDescription.setText(detail.description)
                player.loadVideo(detail.videoId)
                playingVideoDetail = detail
            }

            // Save the Detail to SharedPreference so we can start there next time
            val preferences = getPreferences(Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString(getString(R.string.currentVideoId), detail.videoId)
            editor.commit()
        }
    }
}
