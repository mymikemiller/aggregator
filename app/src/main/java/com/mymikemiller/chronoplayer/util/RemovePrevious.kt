package com.mymikemiller.chronoplayer.util

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.mymikemiller.chronoplayer.Detail
import android.content.ContentValues.TAG
import com.mymikemiller.chronoplayer.Channel
import java.sql.SQLException

/**
 * This class uses the database to keep track of which videos the user has specified they'd like to skip
 */
class RemovePrevious {
    companion object {

        val DATABASE_VERSION: Int = 4
        val DATABASE_NAME: String = "RemovePrevious"
        val REMOVE_BEFORE_VIDEO_ID_TABLE_NAME: String = "RemovePreviousTable"

        // VideoList columns
        val KEY_CHANNELID: String = "ChannelId"
        val KEY_REMOVE_BEFORE_VIDEOID: String="RemoveBeforeVideoId"

        private val REMOVE_BEFORE_VIDEO_ID_TABLE_CREATE =
                "CREATE TABLE " + REMOVE_BEFORE_VIDEO_ID_TABLE_NAME + " (" +
                        KEY_CHANNELID + " TEXT NOT NULL, " +
                        KEY_REMOVE_BEFORE_VIDEOID + " TEXT NOT NULL UNIQUE);"

        fun filterOutRemoved(context: Context, channel: Channel, details: List<Detail>) : List<Detail> {
            val dbHelper = RemoveBeforeVideoIdOpenHelper(context.applicationContext)
            val videoIdToRemoveBefore = dbHelper.getRemoveBeforeVideoIdFromDb(channel)
            if (videoIdToRemoveBefore.isBlank()) {
                // We never removed any videos, so return them all
                return details
            }

            var includedDetails = mutableListOf<Detail>()
            var found = false
            for(detail in details) {
                if (detail.videoId == videoIdToRemoveBefore)
                    found = true

                if (found) {
                    includedDetails.add(detail)
                }
            }

            return includedDetails
        }

        fun setRemovedBeforeVideo(context: Context, detail: Detail) {
            val dbHelper = RemoveBeforeVideoIdOpenHelper(context.applicationContext)
            dbHelper.setRemoveBeforeVideo(detail)
        }

        fun getRemoveBeforeVideoId(context: Context, channel: Channel
        ): String {
            val dbHelper = RemoveBeforeVideoIdOpenHelper(context.applicationContext)
            val removeBeforeVideoId = dbHelper.getRemoveBeforeVideoIdFromDb(channel)
            return removeBeforeVideoId
        }

        fun unRemove(context: Context, channel: Channel) {
            val dbHelper = RemoveBeforeVideoIdOpenHelper(context.applicationContext)
            dbHelper.unRemove(channel)
        }
    }

    class RemoveBeforeVideoIdOpenHelper internal constructor(context: Context) : SQLiteOpenHelper(context, RemovePrevious.DATABASE_NAME, null, RemovePrevious.DATABASE_VERSION) {

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion != newVersion) {
                // Simplest implementation is to drop all old tables and recreate them
                db.execSQL("DROP TABLE IF EXISTS " + RemovePrevious.REMOVE_BEFORE_VIDEO_ID_TABLE_NAME)
                onCreate(db)
            }
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(RemovePrevious.REMOVE_BEFORE_VIDEO_ID_TABLE_CREATE)
        }

        // Set the VideoId in the database
        fun setRemoveBeforeVideo(detail: Detail) {

            // First remove the current removeBeforeVideoId
            unRemove(detail.channel)

            // Create and/or open the database for writing
            val db = writableDatabase

            // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
            // consistency of the database.
            db.beginTransaction()

            try {
                val values = ContentValues()
                values.put(RemovePrevious.KEY_CHANNELID, detail.channel.channelId)
                values.put(RemovePrevious.KEY_REMOVE_BEFORE_VIDEOID, detail.videoId)

                // Notice how we haven't specified the primary key. SQLite auto increments the primary key column.
                db.insertOrThrow(REMOVE_BEFORE_VIDEO_ID_TABLE_NAME, null, values)

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "Error while trying to add Remove Before Video to database")
            } finally {
                db.endTransaction()
            }
        }

        fun getRemoveBeforeVideoIdFromDb(channel: Channel): String {
            var videoId = ""

            // SELECT TOP 1 FROM RemovedBeforeVideoIdTable WHERE ChannelId = channel.channelId
            val REMOVED_BEFORE_SELECT_QUERY = "SELECT * FROM ${RemovePrevious.REMOVE_BEFORE_VIDEO_ID_TABLE_NAME} " +
                    "WHERE $KEY_CHANNELID = ? LIMIT 1"
            val db: SQLiteDatabase
            try {
                db = this.readableDatabase
            } catch (s: SQLException) {
                // We sometimes get an error opening the database.
                // Don't save the VideoId. 's ok. Maybe next time.
                return ""
            }

            val cursor = db.rawQuery(REMOVED_BEFORE_SELECT_QUERY, arrayOf(channel.channelId))
            try {
                if (cursor.moveToFirst()) {
                    videoId = cursor.getString(cursor.getColumnIndex(RemovePrevious.KEY_REMOVE_BEFORE_VIDEOID))
                }
            } catch (e: Exception) {
                Log.d(ContentValues.TAG, "Error while trying to get Remove Before VideoId from database")
            } finally {
                if (cursor != null && !cursor.isClosed) {
                    cursor.close()
                }
                db.close()
            }

            return videoId
        }


        fun unRemove(channel: Channel) {
            val db: SQLiteDatabase
            try {
                db = this.writableDatabase
            } catch (s: SQLException) {
                // Hopefully this doesn't happen...
                return
            }
            db.beginTransaction()
            try {
                // Order of deletions is important when foreign key relationships exist.
                db.delete(RemovePrevious.REMOVE_BEFORE_VIDEO_ID_TABLE_NAME, KEY_CHANNELID + "= ?", arrayOf(channel.channelId))
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.d(ContentValues.TAG, "Error while trying to delete Removed VideoId")
            } finally {
                db.endTransaction()
                db.close()
            }
        }
    }
}