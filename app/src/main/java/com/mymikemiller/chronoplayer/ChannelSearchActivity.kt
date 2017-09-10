package com.mymikemiller.chronoplayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button

/**
 * The page prompting the user to search for a YouTube channel
 */
class ChannelSearchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel_search)

        findViewById<Button>(R.id.search_button).setOnClickListener({
            val searchIntent = Intent(this, MainActivity::class.java);
            startActivity(searchIntent)

            // prevent the user from getting back to this activity
            finish()
        })
    }
}