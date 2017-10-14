package com.mymikemiller.chronoplayer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import com.mymikemiller.chronoplayer.util.Channels
import com.mymikemiller.chronoplayer.util.PlaylistChannels
import com.mymikemiller.chronoplayer.yt.YouTubeAPI
import kotlinx.android.synthetic.main.activity_main.*
import android.widget.ArrayAdapter



/**
 * Created by mikem on 10/14/2017.
 */
class PlaylistChooserActivity: Activity() {

    lateinit var mListView: ListView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_chooser)

        val channels = mutableListOf<Channel>()
        mListView = findViewById<ListView>(R.id.listView)

        val items = listOf("gg", "aa")

        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items)
        mListView.setAdapter(adapter)

        mListView.onItemClickListener = object: AdapterView.OnItemClickListener {
            override fun onItemClick(l: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val playlistTitle = mListView.adapter.getItem(position) as String

                launchMainActivity(playlistTitle)
            }
        }
    }

    // Launch the main activity
    fun launchMainActivity(playlistTitle: String) {
        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        // TODO: get the actual playlist name from the user entering it, instead of from this channelsearchactivity
        mainIntent.putExtra(getString(R.string.extraLaunchPlaylistTitle),
                playlistTitle)
        startActivity(mainIntent)
        finish()
    }
}