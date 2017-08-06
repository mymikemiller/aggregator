package com.mymikemiller.gamegrumpsplayer

import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerView

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

/**
 * A video player allowing users to watch Game Grumps episodes in chronological order while providing the ability to skip entire series.
 */
class MainActivity : YouTubeFailureRecoveryActivity(), View.OnClickListener, YouTubePlayer.OnFullscreenListener {
    private var baseLayout: LinearLayout? = null
    private var playerView: YouTubePlayerView? = null
    private var player: YouTubePlayer? = null
    private var otherViews: View? = null
    private var fullscreen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        baseLayout = findViewById<LinearLayout>(R.id.layout)
        playerView = findViewById<YouTubePlayerView>(R.id.player)
        otherViews = findViewById(R.id.other_views)

        playerView!!.initialize(DeveloperKey.DEVELOPER_KEY, this)

        doLayout()
    }

    override fun onInitializationSuccess(provider: YouTubePlayer.Provider, player: YouTubePlayer,
                                         wasRestored: Boolean) {
        this.player = player
        // Specify that we want to handle fullscreen behavior ourselves.
        player.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_CUSTOM_LAYOUT)
        player.setOnFullscreenListener(this)

        var controlFlags = player!!.fullscreenControlFlags
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        controlFlags = controlFlags or YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE
        player!!.fullscreenControlFlags = controlFlags

        if (!wasRestored) {
            player.cueVideo("avP5d16wEp0")
        }
    }

    override val youTubePlayerProvider: YouTubePlayer.Provider
        get() = playerView as YouTubePlayer.Provider //To change initializer of created properties use File | Settings | File Templates.

    override fun onClick(v: View) {
        player!!.setFullscreen(!fullscreen)
    }

    private fun doLayout() {
        val playerParams = playerView!!.layoutParams as LinearLayout.LayoutParams
        if (fullscreen) {
            // When in fullscreen, the visibility of all other views than the player should be set to
            // GONE and the player should be laid out across the whole screen.
            playerParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            playerParams.height = LinearLayout.LayoutParams.MATCH_PARENT

            otherViews!!.visibility = View.GONE
        } else {
            // This layout is up to you - this is just a simple example (vertically stacked boxes in
            // portrait, horizontally stacked in landscape).
            otherViews!!.visibility = View.VISIBLE
            val otherViewsParams = otherViews!!.layoutParams
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                otherViewsParams.width = 0
                playerParams.width = otherViewsParams.width
                playerParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                otherViewsParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                playerParams.weight = 1f
                baseLayout!!.orientation = LinearLayout.HORIZONTAL
            } else {
                otherViewsParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                playerParams.width = otherViewsParams.width
                playerParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                playerParams.weight = 0f
                otherViewsParams.height = 0
                baseLayout!!.orientation = LinearLayout.VERTICAL
            }
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

}
