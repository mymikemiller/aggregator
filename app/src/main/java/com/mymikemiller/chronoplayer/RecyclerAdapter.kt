package com.mymikemiller.chronoplayer

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.*

import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.LightingColorFilter
import android.graphics.Color
import android.support.v4.content.ContextCompat
import com.google.api.client.util.DateTime


class RecyclerAdapter(private val context: Context,
                      var details: List<Detail>,
                      private val isSelectedCallback: (detail: Detail) -> Boolean,
                      private val onItemClickCallback: (detail: Detail) -> Unit,
                      private val removePreviousCallback: ((date: DateTime) -> Unit)? = null)
        : RecyclerView.Adapter<RecyclerAdapter.DetailHolder>()
{

    class DetailHolder
    (v: View, context: Context) : RecyclerView.ViewHolder(v), View.OnClickListener {
        private val mContext = context
        private val mRootLayout: LinearLayout
        private val mThumbnail: ImageView
        private val mDescription: TextView
        private val mTitle: TextView
        private val mDate: TextView
        private var mMenuButton: ImageView
        private var mTitleTextView: TextView
        private var mDescriptionTextView: TextView
        private var mDateTextView: TextView
        private lateinit var mDetail: Detail
        private lateinit var mIsSelectedCallback: (detail: Detail) -> Boolean
        private lateinit var mOnItemClickCallback: (detail: Detail) -> Unit

        init {
            mRootLayout = v.findViewById(R.id.rootLayout)
            mThumbnail = v.findViewById<ImageView>(R.id.thumbnail)
            mDescription = v.findViewById<TextView>(R.id.description)
            mTitle = v.findViewById<TextView>(R.id.title)
            mDate = v.findViewById<TextView>(R.id.date)
            mMenuButton = v.findViewById(R.id.video_menu_button)
            mTitleTextView = v.findViewById(R.id.title)
            mDescriptionTextView = v.findViewById(R.id.description)
            mDateTextView = v.findViewById(R.id.date)

            v.setOnClickListener(this)
        }

        fun setIsSelectedCallback(callback: (detail: Detail) -> Boolean) {
            mIsSelectedCallback = callback
        }
        fun setOnItemClickCallback(callback: (detail: Detail) -> Unit) {
            mOnItemClickCallback = callback
        }

        fun highlight() {
            mRootLayout.setBackgroundResource(R.color.dark_background)
            setTextColor(ContextCompat.getColor(mContext, R.color.light_font))
        }
        fun unhighlight() {
            mRootLayout.setBackgroundResource(R.color.light_background)
            setTextColor(ContextCompat.getColor(mContext, R.color.dark_gray))
        }

        fun setTextColor(c: Int) {
            mTitleTextView.setTextColor(c)
            mDateTextView.setTextColor(c)
            mDescriptionTextView.setTextColor(c)
        }

        override fun onClick(v: View) {
            mOnItemClickCallback(mDetail)
        }

        fun bindDetail(context: Context,
                       detail: Detail,
                       removePrevious: ((date: DateTime) -> Unit)?) {
            mDetail = detail
            Picasso.with(mThumbnail.context).load(detail.thumbnail).into(mThumbnail)

            mTitle.setText(detail.title)
            mDescription.setText(detail.description)

            val format = SimpleDateFormat("E MMM dd, yyyy", Locale.US)
            val date = Date(detail.dateUploaded.value)
            val dateString = format.format(date)
            mDate.setText(dateString)

            mMenuButton.colorFilter = LightingColorFilter(Color.LTGRAY, Color.LTGRAY)

            mMenuButton.setOnClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    //Creating the instance of PopupMenu
                    val popup = PopupMenu(context, mMenuButton)

                    //Inflating the Popup using xml file
                    popup.menuInflater.inflate(R.menu.recyclerview_item_popup, popup.menu)

                    if (removePrevious == null) {
                        popup.getMenu().findItem(R.id.remove_previous).isVisible = false
                    }

                    //registering popup with OnMenuItemClickListener
                    popup.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                        override fun onMenuItemClick(item: MenuItem): Boolean {

                            if (item.itemId == R.id.remove_previous) {
                                // Remove everything before the clicked video
                                if (removePrevious != null) {
                                    removePrevious(detail.dateUploaded)
                                }
                            }
                            return true
                        }
                    })

                    popup.show()
                }
            })

            if (mIsSelectedCallback(mDetail)) {
                highlight()
            } else {
                unhighlight()
            }
        }

        companion object {
            private val DETAIL_KEY = "DETAIL"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerAdapter.DetailHolder {
        val inflatedView = LayoutInflater.from(parent.context)
                .inflate(R.layout.recyclerview_item_row, parent, false)

        return DetailHolder(inflatedView, parent.context)
    }

    override fun onBindViewHolder(holder: RecyclerAdapter.DetailHolder, position: Int) {
        val itemDetail = details[position]
        holder.setIsSelectedCallback(isSelectedCallback)
        holder.setOnItemClickCallback(onItemClickCallback)
        holder.bindDetail(context, itemDetail, removePreviousCallback)
    }

    override fun getItemCount(): Int {
        return details.size
    }
}
