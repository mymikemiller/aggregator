package com.mymikemiller.chronoplayer.util

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.mymikemiller.chronoplayer.Channel
import com.mymikemiller.chronoplayer.Detail
import java.sql.SQLException

/**
 *  Keeps track of which episode was last played for the purposes of starting with that video when the user loads a channel.
 */
class LastPlayedVideo {
    companion object {
        // Increment this when the table definition changes
        val DATABASE_VERSION: Int = 1
        val DATABASE_NAME: String = "LastPlayedVideo"
        val LAST_PLAYED_VIDEO_TABLE_NAME: String = "LastPlaedVideoTables"

        // VideoList columns
        val KEY_CHANNEL_ID: String="Channel_Id"
        val KEY_LAST_PLAYED_VIDEO_ID: String = "Last_Played_Video"

        private val LAST_PLAYED_VIDEO_TABLE_CREATE =
                "CREATE TABLE " + LAST_PLAYED_VIDEO_TABLE_NAME + " (" +
                        KEY_CHANNEL_ID + " TEXT NOT NULL UNIQUE, " +
                        KEY_LAST_PLAYED_VIDEO_ID + " INTEGER);"

        // This will return the last played videoId from the specified channel
        fun getLastPlayedVideoId (context: Context, channel: Channel): String{
            val dbHelper = LastPlayedVideoOpenHelper(context.applicationContext)
            return dbHelper.getLastPlayedVideoId(channel)
        }

        fun addOrUpdateLastPlayedVideo(context: Context, channel: Channel, detail: Detail) {
            val dbHelper = LastPlayedVideoOpenHelper(context.applicationContext)
            return dbHelper.addOrUpdateLastPlayedVideoId(channel, detail)

        }
    }

    class LastPlayedVideoOpenHelper internal constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion != newVersion) {
                // Simplest implementation is to drop all old tables and recreate them
                db.execSQL("DROP TABLE IF EXISTS " + LAST_PLAYED_VIDEO_TABLE_NAME)
                onCreate(db)
            }
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(LAST_PLAYED_VIDEO_TABLE_CREATE)
        }

        fun getLastPlayedVideoId(channel: Channel): String {
            // If we don't find any rows, lastPlayedVideo should be "" to show that we didn't find any
            var lastPlayedVideoId = ""

            // SELECT
            //  ISNULL(MAX(LastPlayedVideo), VideoId)
            // FROM
            //  Users
            // WHERE
            //  Id = @UserId
            val SELECT_QUERY = "SELECT $KEY_LAST_PLAYED_VIDEO_ID" +
                    " FROM $LAST_PLAYED_VIDEO_TABLE_NAME WHERE $KEY_CHANNEL_ID='${channel.channelId}'"

            //val SELECT_QUERY = "SELECT * FROM $WATCHED_MILLIS_TABLE_NAME"
            val db: SQLiteDatabase
            try {
                db = this.readableDatabase
            } catch (s: SQLException) {
                // We sometimes get an error opening the database.
                // Don't get the watched time, just return "".
                return ""
            }

            val cursor = db.rawQuery(SELECT_QUERY, null)
            try {
                if (cursor.moveToFirst()) {
                    lastPlayedVideoId = cursor.getString(cursor.getColumnIndex(KEY_LAST_PLAYED_VIDEO_ID))
                }
            } catch (e: Exception) {
                Log.d(ContentValues.TAG, "Error while trying to get watched millis from database")
            } finally {
                if (cursor != null && !cursor.isClosed) {
                    cursor.close()
                }

                db.close()
            }

            return lastPlayedVideoId
        }

        // Add or update the watched time.
        fun addOrUpdateLastPlayedVideoId(channel: Channel, lastPlayedVideo: Detail) {
            val db: SQLiteDatabase
            try {
                db = this.writableDatabase
            } catch (s: SQLException) {
                // We sometimes get an error opening the database.
                // Don't save the last played video. 's ok. Maybe next time.
                return
            }

            val initialValues = ContentValues()
            initialValues.put(KEY_CHANNEL_ID, channel.channelId)
            initialValues.put(KEY_LAST_PLAYED_VIDEO_ID, lastPlayedVideo.videoId)

            //insertWithOnConflict returns the row ID of the newly inserted row OR -1 if an error occured (such as it's already in there_
            // This is some magic that will insert, and if it can't, it'll update
            val id = db.insertWithOnConflict(LAST_PLAYED_VIDEO_TABLE_NAME, null, initialValues, SQLiteDatabase.CONFLICT_IGNORE)
            if (id == (-1).toLong()) {
                db.update(LAST_PLAYED_VIDEO_TABLE_NAME, initialValues, "$KEY_CHANNEL_ID =?", arrayOf(channel.channelId))  // number 1 is the _id here, update to variable for your code
            }

            db.close()
        }
    }
}