package com.mymikemiller.gamegrumpsplayer

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_watch_history.*
import android.content.Intent
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.google.api.client.util.DateTime


class WatchHistoryActivity : AppCompatActivity() {

    private lateinit var mHistoryView: RecyclerView
    private lateinit var mAdapter: HistoryRecyclerAdapter
    private lateinit var mDetails: List<Detail>
    private lateinit var mLinearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_history)
        setSupportActionBar(toolbar)
        mDetails = mutableListOf() // get the list of details from the bundle

        val d = Detail(
                "xxx",
                "Kirby Super Star - Spring Breeze ADVENTURES! - GameGrumps",
                "", "https://i.ytimg.com/vi/4wSB3AsSDyA/sddefault.jpg", DateTime(0))
        val details = mDetails.toMutableList()
        details.add(d)
        mDetails = details

        mAdapter = HistoryRecyclerAdapter(this, mDetails, onItemClick)

        mHistoryView = findViewById(R.id.historyView)
        mHistoryView.setAdapter(mAdapter)

        mLinearLayoutManager = LinearLayoutManager(this)
        mHistoryView.setLayoutManager(mLinearLayoutManager)
    }

    // Set up what happens when an playlist item is clicked
    val onItemClick: (Detail) -> Unit = {detail ->
        run {
            //call finish with the detail passed through (parceled)

        }
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
