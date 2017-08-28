package com.mymikemiller.gamegrumpsplayer

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.*

import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.LightingColorFilter
import android.graphics.ColorFilter
import android.R.attr.button
import android.graphics.Color


class RecyclerAdapter(private val context: Context,
                      var details: List<Detail>,
                      private val isSelectedCallback: (detail: Detail) -> Boolean,
                      private val onItemClickCallback: (detail: Detail) -> Unit,
                      private val skipGameCallback: ((game: String) -> Unit)? = null,
                      private val unskipGameCallback: ((game: String) -> Unit)? = null)
        : RecyclerView.Adapter<RecyclerAdapter.DetailHolder>()
{

    class DetailHolder
    (v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        private val mRootLayout: LinearLayout
        private val mThumbnail: ImageView
        private val mGame: TextView
        private val mTitle: TextView
        private val mDate: TextView
        private var mMenuButton: ImageView
        //private var mSkipGameMenuItem: MenuItem
        private lateinit var mDetail: Detail
        private lateinit var mIsSelectedCallback: (detail: Detail) -> Boolean
        private lateinit var mOnItemClickCallback: (detail: Detail) -> Unit

        init {
            mRootLayout = v.findViewById(R.id.rootLayout)
            mThumbnail = v.findViewById<ImageView>(R.id.thumbnail)
            mGame = v.findViewById<TextView>(R.id.game)
            mTitle = v.findViewById<TextView>(R.id.title)
            mDate = v.findViewById<TextView>(R.id.date)
            mMenuButton = v.findViewById(R.id.game_menu_button)
//            mSkipGameMenuItem = v.findViewById<View>(R.id.skip_game) as MenuItem

            v.setOnClickListener(this)
        }

        fun setIsSelectedCallback(callback: (detail: Detail) -> Boolean) {
            mIsSelectedCallback = callback
        }
        fun setOnItemClickCallback(callback: (detail: Detail) -> Unit) {
            mOnItemClickCallback = callback
        }

        fun highlight() {
            mRootLayout.setBackgroundResource(R.color.orange)
        }
        fun unhighlight() {
            mRootLayout.setBackgroundResource(R.color.light_font)
        }

        override fun onClick(v: View) {
            mOnItemClickCallback(mDetail)
        }

        fun bindDetail(context: Context,
                       detail: Detail,
                       skipGameCallback: ((game: String) -> Unit)?,
                       unSkipGameCallback: ((game: String) -> Unit)?) {
            mDetail = detail
            Picasso.with(mThumbnail.context).load(detail.thumbnail).into(mThumbnail)

            val part = if (detail.part.length > 0) " (" + detail.part + ")" else ""
            val fullTitle = detail.game + " " + part
            mGame.setText(fullTitle)
            mTitle.setText(detail.title)

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

                    if (skipGameCallback == null) {
                        popup.getMenu().findItem(R.id.skip_game).isVisible = false
                    }
                    if (unSkipGameCallback == null) {
                        popup.getMenu().findItem(R.id.unskip_game).isVisible = false
                    }

                    //registering popup with OnMenuItemClickListener
                    popup.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                        override fun onMenuItemClick(item: MenuItem): Boolean {

                            if (item.itemId == R.id.unskip_game) {
                                // TODO: unskip game
                            } else if (item.itemId == R.id.skip_game) {
                                // Skip the clicked game
                                if (skipGameCallback != null) {
                                    skipGameCallback(detail.game)
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

        return DetailHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: RecyclerAdapter.DetailHolder, position: Int) {
        val itemDetail = details[position]
        holder.setIsSelectedCallback(isSelectedCallback)
        holder.setOnItemClickCallback(onItemClickCallback)
        holder.bindDetail(context, itemDetail, skipGameCallback, null)
    }

    override fun getItemCount(): Int {
        return details.size
    }
}
