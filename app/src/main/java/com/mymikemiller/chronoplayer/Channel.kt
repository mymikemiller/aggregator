package com.mymikemiller.chronoplayer

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

/**
 * Represents a channel on YouTube
 */
data class Channel(val name: String, val id: String, val thumbnail: String) : Serializable {
    val uploadPlaylistId: String by lazy {
        // We have to replace the second character with a "U" to translate from channelID to
        // upload playlist ID. See https://stackoverflow.com/questions/46158127/youtube-api-get-upload-playlistid-for-youtube-channel

        val first = id.substring(0, 1)
        val last = id.substring(2, id.length)
        first + "U" + last
    }
}