package com.mymikemiller.gamegrumpsplayer

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.api.client.util.DateTime

import com.squareup.picasso.Picasso
import java.util.*

/**
 *
 */

class RecyclerAdapter(private val mDetailsTemp: MutableList<Detail>) : RecyclerView.Adapter<RecyclerAdapter.DetailHolder>() {

    private val mDetails = mDetailsTemp
    init {
        mDetails.add(Detail("aoijoake", "hello", "Description", "test", DateTime(123456)))
    }

    //1
    class DetailHolder//4
    (v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        //2
        private val mItemImage: ImageView
        private val mItemDate: TextView
        private val mItemDescription: TextView
        private var mDetail: Detail? = null

        init {

            mItemImage = v.findViewById<ImageView>(R.id.item_image) as ImageView
            mItemDate = v.findViewById<TextView>(R.id.item_date) as TextView
            mItemDescription = v.findViewById<TextView>(R.id.item_description) as TextView
            v.setOnClickListener(this)
        }

        //5
        override fun onClick(v: View) {
            // Play the current video
        }

        fun bindDetail(detail: Detail) {
            mDetail = detail
            Picasso.with(mItemImage.context).load(detail.thumbnail).into(mItemImage)
            //mItemDate.setText(detail.getHumanDate())
            mItemDescription.setText(detail.description)
        }

        companion object {

            //3
            private val DETAIL_KEY = "DETAIL"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerAdapter.DetailHolder {
        val inflatedView = LayoutInflater.from(parent.context)
                .inflate(R.layout.recyclerview_item_row, parent, false)
        return DetailHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: RecyclerAdapter.DetailHolder, position: Int) {
        val itemDetail = mDetails[position]
        holder.bindDetail(itemDetail)
    }

    override fun getItemCount(): Int {
        return mDetails.size
    }
}
