package com.mymikemiller.chronoplayer

import android.os.Parcel
import android.os.Parcelable
import com.google.api.client.util.DateTime

/**
*   Contains detailed information about the video, e.g. the thumbnail image, title and description
*/
data class Detail(val channel: Channel?,
                       val videoId: String,
                       val title: String,
                       val description: String,
                       val thumbnail: String,
                       val dateUploaded: DateTime,
                       val isPlaylistDetail: Boolean = false) : Comparable<Detail>, Parcelable {

    constructor(parcel: Parcel) : this(
            channel = parcel.readSerializable() as Channel,
            videoId = parcel.readString(),
            title = parcel.readString(),
            description = parcel.readString(),
            thumbnail = parcel.readString(),
            dateUploaded = DateTime(parcel.readLong()),
            isPlaylistDetail = parcel.readInt() != 0) {}

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

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(channel);
        parcel.writeString(videoId)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(thumbnail)
        parcel.writeLong(dateUploaded.value)
        parcel.writeInt(if (isPlaylistDetail) 1 else 0);
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Detail> {
        override fun createFromParcel(parcel: Parcel): Detail {
            return Detail(parcel)
        }

        override fun newArray(size: Int): Array<Detail?> {
            return arrayOfNulls(size)
        }
    }
}
