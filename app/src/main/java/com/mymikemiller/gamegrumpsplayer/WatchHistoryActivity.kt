package com.mymikemiller.gamegrumpsplayer

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_watch_history.*
import android.content.Intent



class WatchHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_history)
        setSupportActionBar(toolbar)

    }

    override fun onBackPressed() {
        setIntents()
        super.onBackPressed()
    }

    private fun setIntents() {
        val result = Intent()
        result.putExtra("videoToPlay", "osanalkd")
        setResult(Activity.RESULT_OK, result)
        finish()
    }

}
