package com.mymikemiller.aggregator

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.content.Context
import android.support.v7.widget.SearchView
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.mymikemiller.aggregator.util.Channels
import com.mymikemiller.aggregator.yt.YouTubeAPI


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

        mSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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

        mListView.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(l: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val channel = mListView.adapter.getItem(position) as Channel
                // Add the channel to the database so it's there when we add to PlaylistChannels table
                Channels.addChannel(this@ChannelSearchActivity, channel)
                sendResult(channel)
            }
        }
    }

    // Launch the main activity
    fun sendResult(channel: Channel) {
        val i = Intent()
        i.putExtra("channel", channel)
        setResult(RESULT_OK, i)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Do nothing to prevent a crash in onActivityResult in MainActivity. So we just don't allow back to be pressed.
    }
}