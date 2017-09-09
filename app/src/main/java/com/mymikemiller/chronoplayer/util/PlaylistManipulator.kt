package com.mymikemiller.chronoplayer.util

import android.content.Context
import android.preference.PreferenceManager
import com.google.api.client.util.DateTime
import com.mymikemiller.chronoplayer.Detail
import com.mymikemiller.chronoplayer.R

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