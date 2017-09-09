package com.mymikemiller.gamegrumpsplayer

import com.google.api.client.util.DateTime

/**
*   Contains detailed information about the video, e.g. the thumbnail image, title and description
*/
data class Detail(val videoId: String,
                  val title: String,
                  val description: String,
                  val thumbnail: String,
                  val dateUploaded: DateTime) : Comparable<Detail> {

    override fun equals(other: Any?): Boolean {
        if (other != null && other is Detail) {
            val otherDetail = other
            return videoId == otherDetail.videoId
        }
        return false
    }

    override fun hashCode(): Int {
        return videoId.hashCode()
    }

    override fun toString(): String {
        return "$title: ($videoId)"
    }
    override fun compareTo(other: Detail): Int {
        return dateUploaded.value.compareTo(other.dateUploaded.value)
    }

    companion object {
    }
}
