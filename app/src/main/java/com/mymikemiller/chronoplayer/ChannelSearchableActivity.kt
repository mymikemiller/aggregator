package com.mymikemiller.chronoplayer

import android.app.Activity
import android.app.ListActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.app.SearchManager
import com.google.android.youtube.player.YouTubeApiServiceUtil
import com.mymikemiller.chronoplayer.yt.YouTubeAPI


/**
 * The page prompting the user to search for a YouTube channel
 */
class ChannelSearchableActivity : ListActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_channel_search)

        // Get the intent, verify the action and get the query
        val intent = intent
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            YouTubeAPI.fetchChannels(query, {channels -> run {
                println(channels)
            }})
        }
    }


}