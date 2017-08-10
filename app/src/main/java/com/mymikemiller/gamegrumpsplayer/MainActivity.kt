package com.mymikemiller.gamegrumpsplayer

import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerView

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.mymikemiller.gamegrumpsplayer.yt.YouTubeAPI

/**
 * A video player allowing users to watch Game Grumps episodes in chronological order while providing the ability to skip entire series.
 */
class MainActivity : YouTubeFailureRecoveryActivity(), YouTubePlayer.OnFullscreenListener {
    private lateinit var baseLayout: LinearLayout
    private lateinit var playerView: YouTubePlayerView
    private lateinit var player: YouTubePlayer
    private lateinit var otherViews: View
    private lateinit var episodeTitle: TextView
    private lateinit var episodeDescription: TextView
    private var fullscreen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        baseLayout = findViewById<LinearLayout>(R.id.layout)
        playerView = findViewById<YouTubePlayerView>(R.id.player)
        otherViews = findViewById(R.id.other_views)
        episodeTitle = findViewById<TextView>(R.id.episodeTitle)
        episodeDescription = findViewById<TextView>(R.id.episodeDescription)

        playerView.initialize(DeveloperKey.DEVELOPER_KEY, this)
        doLayout()

        val setRandomVideo: (List<Details>) -> Unit = { detailsList ->
            run {
                val rand = Math.floor(Math.random() * detailsList.size).toInt()
                setVideo(detailsList[rand])
            }
        }
        // channelId for gamegrumps: UU9CuvdOVfMPvKCiwdGKL3cQ
        YouTubeAPI.fetchChannelIdFromChannelName("gamegrumps", {channelId -> run {
            YouTubeAPI.fetchAllDetailsByChannelId(channelId, setRandomVideo)
        }})
    }

    override fun onInitializationSuccess(provider: YouTubePlayer.Provider, player: YouTubePlayer,
                                         wasRestored: Boolean) {
        this.player = player

        // Specify that we want to handle fullscreen behavior ourselves.
        player.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_CUSTOM_LAYOUT)
        player.setOnFullscreenListener(this)

        var controlFlags = player.fullscreenControlFlags
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        controlFlags = controlFlags or YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE
        player.fullscreenControlFlags = controlFlags
    }

    override val youTubePlayerProvider: YouTubePlayer.Provider
        get() = playerView

    private fun doLayout() {
        val playerParams = playerView.layoutParams as LinearLayout.LayoutParams
        if (fullscreen) {
            // When in fullscreen, the visibility of all other views than the player should be set to
            // GONE and the player should be laid out across the whole screen.
            playerParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            playerParams.height = LinearLayout.LayoutParams.MATCH_PARENT

            otherViews.visibility = View.GONE
        } else {
            // vertically stacked boxes in portrait, horizontally stacked in landscape.
            otherViews.visibility = View.VISIBLE
            val otherViewsParams = otherViews.layoutParams
            otherViewsParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            playerParams.width = otherViewsParams.width
            playerParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            playerParams.weight = 0f
            otherViewsParams.height = 0
            baseLayout.orientation = LinearLayout.VERTICAL
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


    fun setVideo(details: Details) {
        runOnUiThread {
            episodeTitle.setText(details.title)
            episodeDescription.setText(details.description)
            player.cueVideo(details.videoId)
        }
    }
}
