package com.mymikemiller.chronoplayer

import java.io.Serializable

/**
 * Represents a channel on YouTube
 */ data class Channel(val name: String, val channelId: String, val uploadPlaylistId: String, val thumbnail: String) : Serializable {}