package com.mymikemiller.chronoplayer.util

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.api.client.util.DateTime
import com.mymikemiller.chronoplayer.Channel
import com.mymikemiller.chronoplayer.Detail
import com.mymikemiller.chronoplayer.yt.YouTubeAPI
import java.sql.SQLException

/**
 * VideoList stores video Details from the given channel in a local SQL database and manages talking to the YouTubeAPI to fetch videos from YouTube when necessary
 */
class Channels {
    companion object {
        // !! When incrementing this, probably also increment PlaylistChannels's version or else
        // playlists might refer to channels not in this database.
        val DATABASE_VERSION: Int = 10
        val DATABASE_NAME: String = "Channels"
        val CHANNELS_TABLE_NAME: String = "ChannelsTable"

        // Channels columns
        //val name: String, val channelId: String, val uploadPlaylistId: String, val thumbnail: String) : Serializable {}
        val KEY_CHANNELID: String="Channel_Id"
        val KEY_NAME: String="Name"
        val KEY_UPLOAD_PLAYLIST_ID: String = "Upload_Playlist_Id"
        val KEY_THUMBNAIL: String = "Thumbnail"

        private val CHANNELS_TABLE_CREATE =
                "CREATE TABLE " + CHANNELS_TABLE_NAME + " (" +
                        KEY_CHANNELID + " TEXT NOT NULL UNIQUE, " +
                        KEY_NAME + " TEXT, " +
                        KEY_UPLOAD_PLAYLIST_ID + " TEXT, " +
                        KEY_THUMBNAIL + " TEXT);"

        // Returns all Channels in the databases
        fun getAllChannelsFromDb(context: Context) : List<Channel>{
            val dbHelper = ChannelsOpenHelper(context.applicationContext)
            return dbHelper.getAllChannelsFromDb()
        }

        fun addChannel(context: Context, channel: Channel) {
            val dbHelper = ChannelsOpenHelper(context.applicationContext)
            dbHelper.addChannel(channel)
        }

        fun getChannel(context: Context, channelId: String) : Channel? {
            val channels = getAllChannelsFromDb(context)

            var returnChannel: Channel? = null
            for(channel in channels) {
                if (channel.channelId == channelId) {
                    returnChannel = channel
                }
            }
            return returnChannel
        }
    }

    class ChannelsOpenHelper internal constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion != newVersion) {
                // Simplest implementation is to drop all old tables and recreate them
                db.execSQL("DROP TABLE IF EXISTS " + CHANNELS_TABLE_NAME)
                onCreate(db)
            }
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CHANNELS_TABLE_CREATE)
        }

        // Insert a Channel into the database
        fun addChannel(channel: Channel) {

            // Create and/or open the database for writing
            val db = writableDatabase

            // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
            // consistency of the database.
            db.beginTransaction()

            try {
                val values = ContentValues()
                values.put(KEY_CHANNELID, channel.channelId)
                values.put(KEY_NAME, channel.name)
                values.put(KEY_UPLOAD_PLAYLIST_ID, channel.uploadPlaylistId)
                values.put(KEY_THUMBNAIL, channel.thumbnail)

                // Notice how we haven't specified the primary key. SQLite auto increments the primary key column.
                db.insertOrThrow(CHANNELS_TABLE_NAME, null, values)

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.d(ContentValues.TAG, "Error while trying to add Channel to database")
            } finally {
                db.endTransaction()
            }
        }

        // Get all Channels from the database
        fun getAllChannelsFromDb(): List<Channel> {
            val allChannels = mutableListOf<Channel>()

            // SELECT * FROM CHANNELS
            val DETAILS_SELECT_QUERY = "SELECT * FROM $CHANNELS_TABLE_NAME"
            val db: SQLiteDatabase

            try {
                db = this.readableDatabase
            } catch (s: SQLException) {
                // We sometimes get an error opening the database.
                // Don't save the watched time. 's ok. Maybe next time.
                return listOf()
            }

            val cursor = db.rawQuery(DETAILS_SELECT_QUERY, arrayOf())
            try {
                if (cursor.moveToFirst()) {
                    do {
                        val channelId = cursor.getString(cursor.getColumnIndex(KEY_CHANNELID))
                        val name = cursor.getString(cursor.getColumnIndex(KEY_NAME))
                        val uploadPlaylistId = cursor.getString(cursor.getColumnIndex(KEY_UPLOAD_PLAYLIST_ID))
                        val thumbnail = cursor.getString(cursor.getColumnIndex(KEY_THUMBNAIL))

                        val channel = Channel(channelId, name, uploadPlaylistId, thumbnail)
                        allChannels.add(channel)
                    } while (cursor.moveToNext())
                }
            } catch (e: Exception) {
                Log.d(ContentValues.TAG, "Error while trying to get channels from database")
            } finally {
                if (cursor != null && !cursor.isClosed) {
                    cursor.close()
                }
                db.close()
            }

            return allChannels
        }

        fun deleteAllChannels() {
            val db = writableDatabase
            db.beginTransaction()
            try {
                // Order of deletions is important when foreign key relationships exist.
                db.delete(CHANNELS_TABLE_NAME, null, null)
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.d(ContentValues.TAG, "Error while trying to delete all channels")
            } finally {
                db.endTransaction()
            }
        }
    }
}