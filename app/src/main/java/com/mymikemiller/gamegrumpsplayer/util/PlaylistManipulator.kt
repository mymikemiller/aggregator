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

        fun getPreferredPlaylistOrder(context: Context): String{
            // Get the preferred display order from Preferences
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
            val playlistOrderPref = sharedPref.getString(context.getString(R.string.pref_playlistOrderKey),
                    context.getString(R.string.pref_playlistOrder_byDateUploaded))
            return playlistOrderPref
        }

        fun orderByGame(details: List<Detail>): List<Detail> {

            // This map is the map that points from a given game to an ordered list of all the episodes of that game.
            // The key is the game's name and the value is the ordered of the episodes of that game
            // of that game
            val map: MutableMap<String, MutableList<Detail>> = mutableMapOf()

             for (detail in details) {
                if (map[detail.game] == null) {
                    map.put(detail.game, mutableListOf<Detail>())
                }
                map[detail.game]!!.add(detail)
            }

            // This is the list of keys in the order of when they first came up in the passed in details list
            val orderedListOfKeys: MutableList<String> = mutableListOf()

            for(detail in details) {
                if (!orderedListOfKeys.contains(detail.game)) {
                    orderedListOfKeys.add(detail.game)
                }
            }

            // Now we go through all the orderedListOfKeys strings putting in all of map's associated list of Details
            val orderedListOfDetails: MutableList<Detail> = mutableListOf<Detail>()
            for (key in orderedListOfKeys) {
                for (detail in map.getValue(key)) {
                    orderedListOfDetails.add(detail)
                }
            }

            return orderedListOfDetails.toList()

        }

        fun orderByDate(details: List<Detail>): List<Detail> {
            return details.sorted()
        }
    }
}