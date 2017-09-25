package com.mymikemiller.chronoplayer

import android.app.Activity
import android.app.ListActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.app.SearchManager
import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SimpleAdapter
import com.google.android.youtube.player.YouTubeApiServiceUtil
import com.mymikemiller.chronoplayer.util.Channels
import com.mymikemiller.chronoplayer.yt.YouTubeAPI


/**
 * The page prompting the user to search for a YouTube channel
 */
class ChannelSearchableActivity : ListActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

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
                launchMainActivity(channel)
            }
        } else {
            // We couldn't find a last-watched-channel (launch channel) in the shared
            // preferences, so pop up the search dialog
            onSearchRequested()
            //TODO: delete this
            //finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            YouTubeAPI.fetchChannels(query, {channels -> run {

                val listAdapter = ChannelAdapter(this, channels)

                runOnUiThread({
                    setListAdapter(listAdapter)
                })
            }})
        }
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)
        val channel = listAdapter.getItem(position) as Channel
        launchMainActivity(channel)
    }

    // Launch the main activity
    fun launchMainActivity(channel: Channel) {

        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        mainIntent.putExtra("channel", channel)
        startActivity(mainIntent)
    }

}