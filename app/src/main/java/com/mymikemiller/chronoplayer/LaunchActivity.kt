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

        // Don't automatically load the last channel if we came from the settings menu to switch
        // channels. Instead, load the search dialog.
        if (intent.hasExtra(getString(R.string.launchedFromSettings))) {
            onSearchRequested()
        } else {

            // Find the last watched ChannelId in SharedPreferences. If it's there, use the new channel
            // database to launch the MainActivity specifying the channel in the intent. If it's not there,
            // let self launch with the button.
            val preferences = getSharedPreferences(getString(R.string.sharedPrefsName), Context.MODE_PRIVATE)
            val channelId = preferences.getString(getString(R.string.launchChannel), "")

            if (channelId != "") {
                // We have a last watched chanel, so launch the MainActivity
                // Search the database for the specified channelId
                // There should definitely be a channel in Channels for the channelId stored in
                // sharedPreferences, but if there isn't we just continue loading
                val channel = Channels.getChannel(applicationContext, channelId)
                if (channel != null) {
                    // Launch the main activity
                    val mainIntent = Intent(this, MainActivity::class.java);
                    mainIntent.putExtra("channel", channel)
                    startActivity(mainIntent)
                }
            }
        }

        findViewById<Button>(R.id.search_button).setOnClickListener({
            onSearchRequested()
        })
    }

    // Returns the lastWatchVideo's channel if there is a lastPlayedVideo, otherwise null (null
    // happens when we haven't played a channel yet i.e. the first time we launch the app)
//    private fun getLaunchChannel(): Channel? {
//        // Launch the main activity
//        val preferences = getPreferences(Context.MODE_PRIVATE)
//        val channelId = preferences.getString(getString(R.string.launchChannel), "")
//
//        find detail now by searching the channel database for the one with the channelId
//
//        // find the channel and return it or null if none found
//        // We use VideoList to get all detail, and find the first with the correct channelId, as that one will contain the correct channel
//        val details = VideoList.getAllDetailsFromDb(this, detail)
//
//
//
//
//
//    }
}
