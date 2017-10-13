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
 * WatchHistory stores the watched video Details in a local SQL database
 */
class WatchHistory {
    companion object {
        // Increment this when the table definition changes
        val DATABASE_VERSION: Int = 8
        val DATABASE_NAME: String = "WatchHistory"
        val TABLE_NAME: String = "WatchHistoryTable"

        // WatchHistory columns
        val KEY_CHANNELID: String = "Channel_Id"
        val KEY_VIDEOID: String = "Video_Id"
        val KEY_TITLE: String = "Title"
        val KEY_DESCRIPTION: String = "Description"
        val KEY_THUMBNAIL: String = "Thumbnail"
        val KEY_DATE_UPLOADED: String = "Date_Uploaded"

        private val DETAILS_TABLE_CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        KEY_CHANNELID + " TEXT, " +
                        KEY_VIDEOID + " TEXT, " +
                        KEY_TITLE + " TEXT, " +
                        KEY_DESCRIPTION + " TEXT, " +
                        KEY_THUMBNAIL + " TEXT, " +
                        KEY_DATE_UPLOADED + " TEXT);"

        // This will return all the Details from the specified channel currently in the database.
        // in oldest to newest order.
        fun getWatchHistory(context: Context, channel: Channel): List<Detail> {
            val dbHelper = DetailsOpenHelper(context.applicationContext)

            return dbHelper.getAllDetailsFromDb(channel.channelId)
        }

        fun addDetail(context: Context, detail: Detail) {
            val dbHelper = DetailsOpenHelper(context.applicationContext)

            return dbHelper.addDetail(detail)
        }

        class DetailsOpenHelper internal constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                if (oldVersion != newVersion) {
                    // Simplest implementation is to drop all old tables and recreate them
                    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
                    onCreate(db)
                }
            }

            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(DETAILS_TABLE_CREATE)
            }

            // Insert a Detail into the database
            fun addDetail(detail: Detail) {

                // Only add the detail if it's not the most recent video in the database
                val existingDetials = getAllDetailsFromDb(detail.channel.channelId)
                if (existingDetials.size > 0 && detail == existingDetials[existingDetials.size - 1])
                    return

                // Create and/or open the database for writing
                val db = writableDatabase

                // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
                // consistency of the database.
                db.beginTransaction()

                try {
                    val values = ContentValues()
                    values.put(KEY_CHANNELID, detail.channel.channelId)
                    values.put(KEY_VIDEOID, detail.videoId)
                    values.put(KEY_TITLE, detail.title)
                    values.put(KEY_DESCRIPTION, detail.description)
                    values.put(KEY_THUMBNAIL, detail.thumbnail)
                    values.put(KEY_DATE_UPLOADED, detail.dateUploaded.toStringRfc3339())

                    // Notice how we haven't specified the primary key. SQLite auto increments the primary key column.
                    db.insertOrThrow(TABLE_NAME, null, values)

                    db.setTransactionSuccessful()
                } catch (e: Exception) {
                    Log.d(ContentValues.TAG, "Error while trying to add Detail to database")
                } finally {
                    db.endTransaction()
                }
            }

            fun getAllDetailsFromDb(channelId: String): List<Detail> {
                val allDetails = mutableListOf<Detail>()

                // SELECT * FROM DETAILS WHERE ChannelId = channelId
                val DETAILS_SELECT_QUERY = "SELECT * FROM $TABLE_NAME " +
                        "WHERE $KEY_CHANNELID = ?"
                    // If we need to know stuff from the VideoList table, we can join to it here.
//                "SELECT * FROM $TABLE_NAME " +
//                        "LEFT JOIN ${VideoList.CHANNELS_TABLE_NAME}" +
//                        "ON  = ${VideoList.CHANNELS_TABLE_NAME}.${VideoList.KEY_PLAYLIST_TITLE} = " +
//                        "${TABLE_NAME}.$KEY_PLAYLIST_TITLE"
//                "WHERE $KEY_PLAYLIST_TITLE = $channelId"
                val db: SQLiteDatabase

                try {
                    db = this.readableDatabase
                } catch (s: SQLException) {
                    // We sometimes get an error opening the database.
                    // Don't save the watched time. 's ok. Maybe next time.
                    return listOf()
                }

                val cursor = db.rawQuery(DETAILS_SELECT_QUERY, arrayOf(channelId))
                try {
                    if (cursor.moveToFirst()) {
                        do {
                            val videoId = cursor.getString(cursor.getColumnIndex(KEY_VIDEOID))
                            val title = cursor.getString(cursor.getColumnIndex(KEY_TITLE))
                            val description = cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION))
                            val thumbnail = cursor.getString(cursor.getColumnIndex(KEY_THUMBNAIL))
                            val dateUploaded = cursor.getString(cursor.getColumnIndex(KEY_DATE_UPLOADED))
                            val dateRfc3339 = DateTime.parseRfc3339(dateUploaded)

                            // We only need the channelId (we actually don't even need that) and thumbnail
                            // here because we're not displaying anything else
                            val newChannel = Channel(channelId,"", "", thumbnail)

                            val newDetail = Detail(newChannel, videoId, title, description, thumbnail, dateRfc3339)
                            allDetails.add(newDetail)
                        } while (cursor.moveToNext())
                    }
                } catch (e: Exception) {
                    Log.d(ContentValues.TAG, "Error while trying to get details from database")
                } finally {
                    if (cursor != null && !cursor.isClosed) {
                        cursor.close()
                    }
                    db.close()
                }

                return allDetails
            }
        }
    }
}