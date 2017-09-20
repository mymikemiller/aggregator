package com.mymikemiller.chronoplayer

import android.app.Activity
import android.app.ListActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.app.SearchManager
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SimpleAdapter
import com.google.android.youtube.player.YouTubeApiServiceUtil
import com.mymikemiller.chronoplayer.yt.YouTubeAPI


/**
 * The page prompting the user to search for a YouTube channel
 */
class ChannelSearchableActivity : ListActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
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

        // Launch the main activity
        val mainIntent = Intent(this, MainActivity::class.java);
        mainIntent.putExtra("channel", channel)
        startActivity(mainIntent)
    }


}