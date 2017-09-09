package com.mymikemiller.gamegrumpsplayer.util

import android.content.Context
import android.preference.PreferenceManager
import com.google.api.client.util.DateTime
import com.mymikemiller.gamegrumpsplayer.Detail
import com.mymikemiller.gamegrumpsplayer.R

/**
 * This file manipulates playlist to be set in the RecylerAdapter, such as when ordering by game
 *
 */

class PlaylistManipulator {
    companion object {
        fun orderByDate(details: List<Detail>): List<Detail> {
            return details.sorted()
        }
    }
}