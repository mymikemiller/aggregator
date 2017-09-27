package com.mymikemiller.chronoplayer

import android.graphics.Typeface
import android.view.ViewGroup
import android.view.LayoutInflater
import android.support.v4.view.PagerAdapter
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.squareup.picasso.Picasso
import me.grantland.widget.AutofitTextView

/**
 *
 */
class EpisodePagerAdapter(private val mActivity: MainActivity, var details: List<Detail>) : PagerAdapter() {

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        val inflater = LayoutInflater.from(mActivity)
        val layout = inflater.inflate(R.layout.episode_view, collection, false) as ViewGroup

        val detail = details[position]

        val standartTypeface: Typeface = Typeface.createFromAsset(mActivity.getAssets(), "fonts/HelveticaNeueBd.ttf")
        val boldTypeface: Typeface = Typeface.createFromAsset(mActivity.getAssets(), "fonts/HelveticaNeueMed.ttf")

        val title:TextView = layout.findViewById<TextView>(R.id.episodeTitle)
        title.setText(detail.title)
        title.setTypeface(boldTypeface)

        val description = layout.findViewById<TextView>(R.id.episodeDescription)
        description.setText(detail.description)
        description.setTypeface(standartTypeface)

        val thumbnail = layout.findViewById<ImageView>(R.id.thumbnail)
        Picasso.with(mActivity).load(detail.thumbnail).into(thumbnail)

        collection.addView(layout)
        return layout
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
        collection.removeView(view as View)
    }

    override fun getCount(): Int {
        return details.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getPageTitle(position: Int): CharSequence {
        return details[position].title
    }
}