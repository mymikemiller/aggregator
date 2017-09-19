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
class SkippedVideos {
    companion object {

        val DATABASE_VERSION: Int = 4
        val DATABASE_NAME: String = "SkippedVideos"
        val SKIPPED_VIDEOS_TABLE_NAME: String = "SkippedVideosTable"

        // VideoList columns
        val KEY_CHANNELID: String = "ChannelId"
        val KEY_VIDEOID: String="VideoId"

        private val SKIPPED_VIDEOS_TABLE_CREATE =
                "CREATE TABLE " + SKIPPED_VIDEOS_TABLE_NAME + " (" +
                        KEY_CHANNELID + " TEXT NOT NULL, " +
                        KEY_VIDEOID + " TEXT NOT NULL UNIQUE);"

        fun filterOutSkipped(context: Context, channel: Channel, details: List<Detail>) : List<Detail> {
            val dbHelper = skippedVideoOpenHelper(context.applicationContext)
            val videoIdsToSkip = dbHelper.getSkippedVideoIdsFromDb(channel)

            val filteredDetails = details.filter {
                !videoIdsToSkip.contains(it.videoId)
            }

            return filteredDetails
        }

        fun addSkippedVideo(context: Context, detail: Detail) {
            val dbHelper = skippedVideoOpenHelper(context.applicationContext)
            dbHelper.addSkippedVideo(detail)
        }

        fun getAllSkippedVideos(context: Context, channel: Channel
        ): List<String> {
            val dbHelper = skippedVideoOpenHelper(context.applicationContext)
            val skippedVideos = dbHelper.getSkippedVideoIdsFromDb(channel)
            return skippedVideos
        }

        fun unSkipAllVideos(context: Context, channel: Channel) {
            val dbHelper = skippedVideoOpenHelper(context.applicationContext)
            dbHelper.unskipAllVideos(channel)
        }
        fun unSkipVideo(context: Context, VideoId: String) {
            val dbHelper = skippedVideoOpenHelper(context.applicationContext)
            dbHelper.unskipVideo(VideoId)
        }
    }

    class skippedVideoOpenHelper internal constructor(context: Context) : SQLiteOpenHelper(context, SkippedVideos.DATABASE_NAME, null, SkippedVideos.DATABASE_VERSION) {

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion != newVersion) {
                // Simplest implementation is to drop all old tables and recreate them
                db.execSQL("DROP TABLE IF EXISTS " + SkippedVideos.SKIPPED_VIDEOS_TABLE_NAME)
                onCreate(db)
            }
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(SkippedVideos.SKIPPED_VIDEOS_TABLE_CREATE)
        }

        // Insert a Video into the database
        fun addSkippedVideo(detail: Detail) {

            // Create and/or open the database for writing
            val db = writableDatabase

            // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
            // consistency of the database.
            db.beginTransaction()

            try {
                val values = ContentValues()
                values.put(SkippedVideos.KEY_CHANNELID, detail.channel.channelId)
                values.put(SkippedVideos.KEY_VIDEOID, detail.videoId)

                // Notice how we haven't specified the primary key. SQLite auto increments the primary key column.
                db.insertOrThrow(SKIPPED_VIDEOS_TABLE_NAME, null, values)

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.d(ContentValues.TAG, "Error while trying to add Skipped Video to database")
            } finally {
                db.endTransaction()
            }
        }
        fun unskipVideo(videoId: String) {
            // Make sure the videoId string doesn't contain single quotes or it will mess the query up.
            // Instead it should have two single quotes
            val theVideoId = videoId.replace("'", "''")

            val db = writableDatabase
            db.beginTransaction()
            try {
                db.delete(SKIPPED_VIDEOS_TABLE_NAME, KEY_VIDEOID + "='" + theVideoId + "'", null)
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.d(TAG, "Error while trying to delete skipped videoId from database")
            } finally {
                db.endTransaction()
            }
        }

        fun getSkippedVideoIdsFromDb(channel: Channel): List<String> {
            val videoIds = mutableListOf<String>()

            // SELECT * FROM SkippedVideosTable WHERE ChannelId = channel.channelId
            val SKIPPED_VIDEOS_SELECT_QUERY = "SELECT * FROM ${SkippedVideos.SKIPPED_VIDEOS_TABLE_NAME} " +
                    "WHERE $KEY_CHANNELID = ?"
            val db: SQLiteDatabase
            try {
                db = this.readableDatabase
            } catch (s: SQLException) {
                // We sometimes get an error opening the database.
                // Don't save the watched time. 's ok. Maybe next time.
                return listOf()
            }

            val cursor = db.rawQuery(SKIPPED_VIDEOS_SELECT_QUERY, arrayOf(channel.channelId))
            try {
                if (cursor.moveToFirst()) {
                    do {
                        val videoId = cursor.getString(cursor.getColumnIndex(SkippedVideos.KEY_VIDEOID))
                        videoIds.add(videoId)
                    } while (cursor.moveToNext())
                }
            } catch (e: Exception) {
                Log.d(ContentValues.TAG, "Error while trying to get skipped videos from database")
            } finally {
                if (cursor != null && !cursor.isClosed) {
                    cursor.close()
                }
                db.close()
            }

            return videoIds.toList()
        }


        fun unskipAllVideos(channel: Channel) {
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
                db.delete(SkippedVideos.SKIPPED_VIDEOS_TABLE_NAME, KEY_CHANNELID + "= ?", arrayOf(channel.channelId))
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.d(ContentValues.TAG, "Error while trying to delete all skipped videos")
            } finally {
                db.endTransaction()
                db.close()
            }
        }
    }
}