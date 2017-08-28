package com.mymikemiller.gamegrumpsplayer

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_unskip_game.*
import android.content.Intent



class UnskipGameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unskip_game)
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
