package com.mymikemiller.chronoplayer

import android.app.Activity
import android.app.ListActivity
import android.content.Intent
import android.os.Bundle
import android.app.SearchManager
import android.content.Context
import android.support.v7.widget.SearchView
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.android.youtube.player.YouTubeApiServiceUtil
import com.mymikemiller.chronoplayer.util.Channels
import com.mymikemiller.chronoplayer.yt.YouTubeAPI
import kotlinx.android.synthetic.main.activity_channel_search.*


/**
 * The page prompting the user to search for a YouTube channel
 */
class ChannelSearchActivity : Activity() {

    lateinit var mListView: ListView
    lateinit var mSearchView: SearchView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel_search)

        val channels = mutableListOf<Channel>()
        mListView = findViewById<ListView>(R.id.listView)
        mListView.setAdapter(ChannelAdapter(this, channels))

        mSearchView = findViewById<SearchView>(R.id.searchView)
        mSearchView.setQueryHint(getString(R.string.channel_search_hint));
        mSearchView.setIconifiedByDefault(false)

        mSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    YouTubeAPI.fetchChannels(query, { channels ->
                        run {

                            val channelAdapter = mListView.adapter as ChannelAdapter

                            runOnUiThread({
                                channelAdapter.clear()
                                channelAdapter.addAll(channels)
                            })
                        }
                    })
                    
                    // Hide the keyboard
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0)
                }

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // do nothing
                return false
            }

        })

        mListView.onItemClickListener = object: AdapterView.OnItemClickListener {
            override fun onItemClick(l: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val channel = mListView.adapter.getItem(position) as Channel
                launchMainActivity(channel)
            }
        }


        if (!intent.hasExtra(getString(R.string.launchedFromSettings))) {
            // We were not launched from settings (we loaded the app for the first time), so next
            // we see if we have a preference for the last channel we watched. Find the last
            // watched ChannelId in SharedPreferences. If it's there, use the channel
            // database to launch the MainActivity specifying the channel in the intent. If it's
            // not there, load this view and let the user search
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
                // preferences, so simply allow this activity to load
            }
        }
    }

    // Launch the main activity
    fun launchMainActivity(channel: Channel) {
        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        mainIntent.putExtra("channel", channel)
        startActivity(mainIntent)
        finish()
    }

}