package com.mymikemiller.chronoplayer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.mymikemiller.chronoplayer.util.Channels
import com.mymikemiller.chronoplayer.util.VideoList

/**
 * The page prompting the user to search for a YouTube channel
 */
class LaunchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
    }

    fun launchCorrectActivity() {

        // Don't automatically load the last channel if we came from the settings menu to switch
        // channels. Instead, load the search dialog.
        if (intent.hasExtra(getString(R.string.launchedFromSettings))) {
            onSearchRequested()
            // Don't "finish" here or it goes back to main activity when selecting "Change Channel" in the settings
            // finish()
        } else {

            // Find the last watched ChannelId in SharedPreferences. If it's there, use the new channel
            // database to launch the MainActivity specifying the channel in the intent. If it's
            // not there, launch the channel selector screen {this activity's search screen)
            val preferences = getSharedPreferences(getString(R.string.sharedPrefsName), Context.MODE_PRIVATE)
            val channelId = preferences.getString(getString(R.string.launchChannel), "")

            if (channelId != "") {
                // We have a last watched channel, so launch the MainActivity
                // Search the database for the specified channelId
                // There should definitely be a channel in Channels for the channelId stored in
                // sharedPreferences, but if there isn't we just continue loading
                val channel = Channels.getChannel(applicationContext, channelId)
                if (channel != null) {
                    // Launch the main activity
                    val mainIntent = Intent(this, MainActivity::class.java);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    mainIntent.putExtra("channel", channel)
                    startActivity(mainIntent)
                    finish()
                }
            } else {
                // We couldn't find a last-watched-channel (launch channel) in the shared
                // preferences, so pop up the search dialog
                onSearchRequested()
            }
        }

        findViewById<Button>(R.id.search_button).setOnClickListener({
            onSearchRequested()
            finish()
        })
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }


    override fun onResume() {
        super.onResume()

        launchCorrectActivity()

    }
}
