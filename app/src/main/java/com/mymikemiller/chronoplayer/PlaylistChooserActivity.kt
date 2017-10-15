package com.mymikemiller.chronoplayer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuInflater
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
import android.R.string.cancel
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Handler
import android.text.InputType
import android.support.v4.widget.SearchViewCompat.setInputType
import android.widget.EditText

/**
 * Created by mikem on 10/14/2017.
 */
class PlaylistChooserActivity: AppCompatActivity() {

    lateinit var mListView: ListView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_chooser)

        // set up the action bar
        val myToolbar = findViewById<View>(R.id.playlist_chooser_toolbar) as Toolbar
        setSupportActionBar(myToolbar)

        mListView = findViewById<ListView>(R.id.listView)

        val items = listOf("gg", "aa")

        val c = Channel("UCVV8ZTEgZwv08TlqpxEkdTg","Game Time", "UUVV8ZTEgZwv08TlqpxEkdTg", "")
        Channels.addChannel(this, c)
//        val c: Channel = Channels.getChannel(this, "UC9CuvdOVfMPvKCiwdGKL3cQ")!!
        PlaylistChannels.addChannel(this, "gg", c)

        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items)
        mListView.setAdapter(adapter)

        mListView.onItemClickListener = object: AdapterView.OnItemClickListener {
            override fun onItemClick(l: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val playlistTitle = mListView.adapter.getItem(position) as String

                launchMainActivity(playlistTitle)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu items for use in the action bar
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.show_my_playlists_menu, menu)
        return true;
    }
    @Override
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar items
        if (item.getItemId() == R.id.action_show_my_playlists) {



            return true;
        } else if (item.getItemId() == R.id.action_add_playlist) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Playlist Name")

            // Set up the input
            val input = EditText(this)
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            builder.setView(input)

            // Set up the buttons
            builder.setNegativeButton("Cancel", { dialog, which -> dialog.cancel() })
            builder.setPositiveButton("OK", { dialog, which ->
                run {
                    launchMainActivity(input.text.toString().trim())
                }
            })

            builder.show()

            // Pop up the keyboard
            val handler = Handler()
            handler.post(Runnable {
                input.requestFocus()
                val mgr = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                mgr.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            })

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Launch the main activity
    fun launchMainActivity(playlistTitle: String) {
        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        mainIntent.putExtra(getString(R.string.extraLaunchPlaylistTitle),
                playlistTitle)
        startActivity(mainIntent)
        finish()
    }
}