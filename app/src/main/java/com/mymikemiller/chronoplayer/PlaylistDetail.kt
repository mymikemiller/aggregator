package com.mymikemiller.chronoplayer

import com.google.api.client.util.DateTime

/**
 * Created by mike on 10/20/17.
 */
// In a PlaylistDetail, the playlistVideoId is the full playlist videoId referring to the video and the playlist it's in.
class PlaylistDetail(channel: Channel?, videoId: String, title: String, description: String, thumbnail: String, dateUploaded: DateTime, val playlistVideoId: String) {
    // A PlaylistDetail has no channel, so we pass null for channel
    val detail = Detail(channel, videoId, title, description, thumbnail, dateUploaded, true)
}