package com.mymikemiller.aggregator.util

import com.mymikemiller.aggregator.Detail

/**
 * This file manipulates playlist to be set in the RecylerAdapter
 *
 */

class PlaylistManipulator {
    companion object {
        fun orderByDate(details: List<Detail>): List<Detail> {
            return details.sorted()
        }
    }
}