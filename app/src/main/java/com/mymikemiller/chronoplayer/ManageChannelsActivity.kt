package com.mymikemiller.chronoplayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.Toast
import com.mymikemiller.chronoplayer.util.PlaylistChannels
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import android.view.KeyEvent.KEYCODE_BACK



/**
 * Allows you to manage which channels end up in the playlist
 */
class ManageChannelsActivity : AppCompatActivity() {

    val CHANNEL_SELECT_REQUEST = 2  // The request code from the ChannelSelectActivity activity

    lateinit var mListView: ListView
    var mPlaylistTitle = ""
    var mChannels = mutableListOf<Channel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_channels)

        // set up the action bar
        val myToolbar = findViewById<View>(R.id.my_toolbar) as Toolbar
        setSupportActionBar(myToolbar)

        // Get the list of channels to display
        mPlaylistTitle = intent.getStringExtra(getString(R.string.playlistTitle))
        mChannels = PlaylistChannels.getChannels(this, mPlaylistTitle).toMutableList()

        mListView = findViewById<ListView>(R.id.listView)
        mListView.setAdapter(ChannelAdapter(this, mChannels))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.manage_channels_action_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.getItemId()

        if (id == R.id.add_channel) {

            val channelSearchActivityIntent = Intent(this, ChannelSearchActivity::class.java)
            channelSearchActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivityForResult(channelSearchActivityIntent, CHANNEL_SELECT_REQUEST)

            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val i: Intent = Intent()
        setResult(Activity.RESULT_OK, i)
        finish()
        super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        // Check which request we're responding to
        if (requestCode == CHANNEL_SELECT_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                // The user selected a new channel to add to our list
                val channel = data.getSerializableExtra("channel") as Channel
                PlaylistChannels.addChannel(this, mPlaylistTitle, channel)

                mChannels.add(channel)
                (mListView.adapter as ChannelAdapter).notifyDataSetChanged()

                val i: Intent = Intent()
                setResult(Activity.RESULT_OK, i)

                finish()
            }
        }
    }
}