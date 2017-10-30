package com.mymikemiller.aggregator

import java.io.Serializable

/**
 * Represents a channel on YouTube
 */ data class Channel(val channelId: String, val name: String, val uploadPlaylistId: String, val thumbnail: String) : Serializable {}