package com.mymikemiller.gamegrumpsplayer.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import java.net.URL

/**
 * This utility downloads a bitmap from a URL and provides a callback to be handled when the bitmap
 * is retrieved
 */
class DownloadImageTask(val callback: (Bitmap) -> Unit) : AsyncTask<String, Void, Bitmap>() {
    override fun doInBackground(vararg urls: String): Bitmap {
        val urldisplay = urls[0]
        var bmp: Bitmap? = null
        try {
            val url = URL(urldisplay)
            val stream = url.openConnection().getInputStream()
            bmp = BitmapFactory.decodeStream(stream)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return bmp!!
    }
    override fun onPostExecute(result: Bitmap) {
        callback(result)
    }
}