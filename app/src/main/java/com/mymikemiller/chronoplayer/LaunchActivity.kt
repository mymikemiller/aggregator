package com.mymikemiller.chronoplayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button

/**
 * The page prompting the user to search for a YouTube channel
 */
class LaunchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        var channel: Channel? = null

        if (intent.hasExtra("channel")) {
            channel = intent.getSerializableExtra("channel") as Channel
        }
        findViewById<Button>(R.id.search_button).setOnClickListener({
            println(channel?.channelId)
            onSearchRequested()
        })
    }
}
