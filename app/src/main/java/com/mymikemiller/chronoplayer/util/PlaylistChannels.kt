package com.mymikemiller.chronoplayer.util

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.api.client.util.DateTime
import com.mymikemiller.chronoplayer.Channel
import com.mymikemiller.chronoplayer.Detail
import java.sql.SQLException

/**
 *  Keeps track of which YouTube channels are to be committed to the specified playlist
 */
class PlaylistChannels {
    companion object {
        val TAG = "PlaylistChannels"

        // Increment this when the table definition changes
        val DATABASE_VERSION: Int = 12
        val DATABASE_NAME: String = "PlaylistChannels"
        val PLAYLIST_CHANNELS_TABLE_NAME: String = "PlaylistChannelsTable"

        // PlaylistChannels columns
        val KEY_PLAYLIST_TITLE: String = "PLAYLIST_TITLE"
        val KEY_CHANNEL_ID: String="CHANNEL_ID"

        private val PLAYLIST_CHANNELS_TABLE_CREATE =
                "CREATE TABLE " + PLAYLIST_CHANNELS_TABLE_NAME + " (" +
                        KEY_PLAYLIST_TITLE + " TEXT NOT NULL, " +
                         KEY_CHANNEL_ID + " TEXT NOT NULL, " +
                        "UNIQUE(" + KEY_PLAYLIST_TITLE + ", " + KEY_CHANNEL_ID + "));"

        // This will return the list of channels the user wants in the given playlist
        fun getChannels(context: Context, playlistTitle: String): List<Channel>{
            val dbHelper = PlaylistChannelsOpenHelper(context.applicationContext)
            return dbHelper.getChannels(context, playlistTitle)
        }

        fun addChannel(context: Context, playlistTitle: String, channel: Channel) {
            val dbHelper = PlaylistChannelsOpenHelper(context.applicationContext)
            return dbHelper.addChannel(playlistTitle, channel)
        }

        fun removeChannel(context: Context, playlistTitle: String, channel: Channel) {
            val dbHelper = PlaylistChannelsOpenHelper(context.applicationContext)
            return dbHelper.removeChannel(playlistTitle, channel)
        }
    }

    class PlaylistChannelsOpenHelper internal constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion != newVersion) {
                // Simplest implementation is to drop all old tables and recreate them
                db.execSQL("DROP TABLE IF EXISTS " + PLAYLIST_CHANNELS_TABLE_NAME)
                onCreate(db)
            }
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(PLAYLIST_CHANNELS_TABLE_CREATE)
        }

        // We use the Channels database to return the actual Channel object, not just a ChannelId.
        // There should definitely be a channel for each channelId in this database because to get
        // a channel to put into this database, it must be in the Channels database
        fun getChannels(context: Context, playlistTitle: String): List<Channel> {
            // If we don't find any rows, PlaylistChannels should be "" to show that we didn't find any
            val channels = mutableListOf<Channel>()

            val SELECT_QUERY = "SELECT $KEY_CHANNEL_ID" +
                    " FROM $PLAYLIST_CHANNELS_TABLE_NAME " +
                    " WHERE $KEY_PLAYLIST_TITLE='${playlistTitle}'"

            val db: SQLiteDatabase
            try {
                db = this.readableDatabase
            } catch (s: SQLException) {
                // We sometimes get an error opening the database.
                // Don't get the watched time, just return empty list.
                return channels.toList()
            }

            val cursor = db.rawQuery(SELECT_QUERY, null)
            try {
                if (cursor.moveToFirst()) {
                    do {
                        val channelId = cursor.getString(cursor.getColumnIndex(KEY_CHANNEL_ID))
                        val channel = Channels.getChannel(context, channelId)
                        if (channel != null) {
                            channels.add(channel)
                        } else {
                            Log.e(TAG, "Channel must exist in Channels database for the specified playlist title " + playlistTitle)
                        }
                    } while (cursor.moveToNext())
                }
            } catch (e: Exception) {
                Log.d(ContentValues.TAG, "Error while trying to get channels from PlaylistChannelss database")
            } finally {
                if (cursor != null && !cursor.isClosed) {
                    cursor.close()
                }

                db.close()
            }

            return channels.toList()
        }

        // Add the channel for the specified playlist.
        fun addChannel(playlistTitle: String, channel: Channel) {
            val db: SQLiteDatabase
            try {
                db = this.writableDatabase
            } catch (s: SQLException) {
                // We sometimes get an error opening the database.
                return
            }

            val initialValues = ContentValues()
            initialValues.put(KEY_PLAYLIST_TITLE, playlistTitle)
            initialValues.put(KEY_CHANNEL_ID, channel.channelId)

            //insertWithOnConflict returns the row ID of the newly inserted row OR -1 if an error occured (such as it's already in there_
            // This is some magic that will insert, and if it can't, it'll update
            val id = db.insert(PLAYLIST_CHANNELS_TABLE_NAME, null, initialValues)

            db.close()
        }

        // Remove the channel from the specified playlist.
        fun removeChannel(playlistTitle: String, channel: Channel) {
            val db: SQLiteDatabase
            try {
                db = this.writableDatabase
            } catch (s: SQLException) {
                // We sometimes get an error opening the database.
                return
            }

            val whereClause = KEY_PLAYLIST_TITLE + "=? AND " + KEY_CHANNEL_ID + "=?"
            val whereArgs = arrayOf<String>(playlistTitle, channel.channelId)
            db.delete(PLAYLIST_CHANNELS_TABLE_NAME, whereClause, whereArgs)

            db.close()
        }
    }
}