package com.mymikemiller.chronoplayer

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_watch_history.*
import android.content.Intent
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.google.api.client.util.DateTime
import com.mymikemiller.chronoplayer.util.VideoList
import com.mymikemiller.chronoplayer.util.WatchHistory


class WatchHistoryActivity : AppCompatActivity() {
    private lateinit var mHistoryView: RecyclerView
    private lateinit var mAdapter: HistoryRecyclerAdapter
    private lateinit var mDetails: List<Detail>
    private lateinit var mLinearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_history)
        setSupportActionBar(toolbar)

        val playlistTitle = intent.getStringExtra("playlistTitle") as String
        mDetails = WatchHistory.getWatchHistory(this, playlistTitle).reversed()

        mAdapter = HistoryRecyclerAdapter(this, mDetails, onItemClick)

        mHistoryView = findViewById(R.id.historyView)
        mHistoryView.setAdapter(mAdapter)

        mLinearLayoutManager = LinearLayoutManager(this)
        mHistoryView.setLayoutManager(mLinearLayoutManager)
    }

    // Set up what happens when a playlist item is clicked
    val onItemClick: (Detail) -> Unit = {detail ->
        run {
            val result = Intent()
            result.putExtra(WATCH_HISTORY_DETAIL, detail.videoId)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    override fun onBackPressed() {
        val result = Intent()
        setResult(Activity.RESULT_CANCELED, result)
        finish()
        super.onBackPressed()
    }

    companion object {
        val WATCH_HISTORY_DETAIL = "WATCH_HISTORY_DETAIL"
    }

}
