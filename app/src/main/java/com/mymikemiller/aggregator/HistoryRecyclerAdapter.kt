package com.mymikemiller.aggregator

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.squareup.picasso.Picasso

class HistoryRecyclerAdapter(private val context: Context,
                             var details: List<Detail>,
                             private val onItemClickCallback: (detail: Detail) -> Unit)
        : RecyclerView.Adapter<HistoryRecyclerAdapter.DetailHolder>()
{

    class DetailHolder
    (v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        private val mRootLayout: LinearLayout
        private val mThumbnail: ImageView
        private val mDescription: TextView
        private val mTitle: TextView
        private val mDate: TextView
        private val mMenu: ImageView
        private lateinit var mDetail: Detail
        private lateinit var mOnItemClickCallback: (detail: Detail) -> Unit

        init {
            mRootLayout = v.findViewById(R.id.rootLayout)
            mThumbnail = v.findViewById<ImageView>(R.id.thumbnail)
            mDescription = v.findViewById<TextView>(R.id.description)
            mTitle = v.findViewById<TextView>(R.id.title)
            mDate = v.findViewById<TextView>(R.id.date)
            mMenu = v.findViewById<ImageView>(R.id.video_menu_button)

            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            mOnItemClickCallback(mDetail)
        }

        fun setOnItemClickCallback(callback: (detail: Detail) -> Unit) {
            mOnItemClickCallback = callback
        }

        fun bindDetail(detail: Detail) {
            mDetail = detail
            Picasso.with(mThumbnail.context).load(detail.thumbnail).into(mThumbnail)

            mDescription.setText(detail.description)
            mTitle.setText(detail.title)

            mDate.visibility = View.GONE
            mMenu.visibility = View.GONE
        }

        companion object {
            private val DETAIL_KEY = "DETAIL"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryRecyclerAdapter.DetailHolder {
        val inflatedView = LayoutInflater.from(parent.context)
                .inflate(R.layout.recyclerview_item_row, parent, false)

        return DetailHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: HistoryRecyclerAdapter.DetailHolder, position: Int) {
        val itemDetail = details[position]
        holder.setOnItemClickCallback(onItemClickCallback)
        holder.bindDetail(itemDetail)
    }

    override fun getItemCount(): Int {
        return details.size
    }
}