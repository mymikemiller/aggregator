package com.mymikemiller.aggregator

import android.content.Context
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import com.squareup.picasso.Picasso

/**
 *
 */
class ChannelAdapter(context: Context, channels: List<Channel>,val deleteCallback: (position: Int) -> Unit = {}) : ArrayAdapter<Channel>(context, 0, channels) {

    var showDeleteIcon = false

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var theConvertView = convertView
        // Get the data item for this position
        val channel = getItem(position)
        // Check if an existing view is being reused, otherwise inflate the view
        if (theConvertView == null) {
            theConvertView = LayoutInflater.from(context).inflate(R.layout.channel_item_row, parent, false)
        }
        // Lookup view for data population
        val titleTextView = theConvertView!!.findViewById<TextView>(R.id.title) as TextView
        val thumbnailImageView = theConvertView.findViewById<ImageView>(R.id.thumbnail) as ImageView
        // Populate the data into the template view using the data object
        titleTextView.setText(channel.name)
        Picasso.with(thumbnailImageView.context).load(channel.thumbnail).into(thumbnailImageView)
        // Return the completed view to render on screen

        if (showDeleteIcon) {
            val deleteIcon = theConvertView.findViewById<ImageView>(R.id.deleteIcon)
            deleteIcon.visibility = View.VISIBLE
            deleteIcon.setOnClickListener {
                deleteCallback(position)
            }
        }

        return theConvertView
    }
}