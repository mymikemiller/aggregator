package com.mymikemiller.chronoplayer.util

import android.content.Context
import com.mymikemiller.chronoplayer.Detail
import com.mymikemiller.chronoplayer.yt.YouTubeAPI
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues.TAG
import android.content.ContentValues
import android.util.Log
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.model.Playlist
import com.mymikemiller.chronoplayer.Channel
import java.sql.SQLException

/**
 * VideoList stores video Details from the given channel in a local SQL database and manages talking to the YouTubeAPI to fetch videos from YouTube when necessary
 */
class VideoList {
    companion object {
        // Increment this when the table definition changes
        val DATABASE_VERSION: Int = 78
        val DATABASE_NAME: String = "VideoList"
        val DETAILS_TABLE_NAME: String = "VideoListTable"

        // VideoList columns
        val KEY_CHANNELID: String="Channel_Id"
        val KEY_VIDEOID: String="Video_Id"
        val KEY_TITLE: String = "Title"
        val KEY_DESCRIPTION: String = "Description"
        val KEY_THUMBNAIL: String = "Thumbnail"
        val KEY_DATE_UPLOADED: String = "Date_Uploaded"

        private val DETAILS_TABLE_CREATE =
                "CREATE TABLE " + DETAILS_TABLE_NAME + " (" +
                        KEY_CHANNELID + " TEXT, " +
                        KEY_VIDEOID + " TEXT NOT NULL UNIQUE, " +
                        KEY_TITLE + " TEXT, " +
                        KEY_DESCRIPTION + " TEXT, " +
                        KEY_THUMBNAIL + " TEXT, " +
                        KEY_DATE_UPLOADED + " TEXT);"

        // This will return the number of Details currently in the database
        fun getNumDetailsInDb(context: Context, playlistTitle: String) : Int {
            val channels = PlaylistChannels.getChannels(context, playlistTitle)
            val dbHelper = DetailsOpenHelper(context.applicationContext)
            return dbHelper.getAllDetailsFromDb(channels).size
        }

        // Returns all Details in the database that belong to the given channel
        fun getAllDetailsFromDb(context: Context,
                                      playlistTitle: String) : List<Detail>{
            val channels = PlaylistChannels.getChannels(context, playlistTitle)
            val dbHelper = DetailsOpenHelper(context.applicationContext)
            return dbHelper.getAllDetailsFromDb(channels)
        }

        // Returns all Details in the database that belong to the given channel
        fun getAllDetailsFromDb(context: Context,
                                channels: List<Channel>) : List<Detail>{
            val dbHelper = DetailsOpenHelper(context.applicationContext)
            return dbHelper.getAllDetailsFromDb(channels)
        }

        fun clearDatabase(context: Context) {
            val dbHelper = DetailsOpenHelper(context.applicationContext)
            return dbHelper.deleteAllDetails()
        }

        // create a member variable (of a new Details Fetcher class) storing the number of times the callback has been called, and all videos. Also store the callback.
        // when the number of times the callback has been called equals the number of channels, call the final callbackj

        fun getDetail(context: Context, channels: List<Channel>, videoId: String) : Detail? {
            val details = getAllDetailsFromDb(context, channels)

            var returnDetail:Detail? = null
            for(detail in details) {
                if (detail.videoId == videoId) returnDetail = detail
            }
            return returnDetail
        }

        fun getDetails(context: Context, channels: List<Channel>) : List<Detail> {
            val details = getAllDetailsFromDb(context, channels)
            return details
        }

        fun fetchAllDetails(context: Context,
                            playlistTitle: String,
                            stopAtDate: DateTime?,
                            setPercentageCallback: (totalVideos: kotlin.Int, currentVideoNumber: kotlin.Int) -> Unit,
                            callbackWhenDone: (details: List<Detail>) -> Unit) {

            val channels = PlaylistChannels.getChannels(context, playlistTitle)

            val fetcher = DetailsFetcher(channels, setPercentageCallback, callbackWhenDone)
            fetcher.startFetch(context, stopAtDate)
        }


        private class DetailsFetcher(private val channels: List<Channel>,
                                     private val setPercentageCallback: (totalVideos: kotlin.Int, currentVideoNumber: kotlin.Int) -> Unit,
                                     private val callbackWhenDoneFetchingAllChannels: (details: List<Detail>) -> Unit) {

            private var numberOfCallbacksReceived = 0
            private val details = mutableListOf<Detail>()
            private var fetchInProgress = false

            fun startFetch(context: Context, stopAtDate: DateTime?) {
                // Only allow one fetch at a time
                if (fetchInProgress) {
                    throw Error("Details fetch already in progress. Can't start another.")
                }
                numberOfCallbacksReceived = 0
                details.clear()
                fetchInProgress = true

                for(channel in channels) {
                    fetchAllDetails(context,
                            channel,
                            stopAtDate,
                            setPercentageCallback,
                            { detailsFetched ->
                                // We get here each time we finish fetching one of the channel's details
                                details.addAll(detailsFetched)
                                // TODO: Figure out what to do with setPercentageCallback. Probably use YoutubeAPI.getNumDetails(Task)
                                numberOfCallbacksReceived++
                                if (numberOfCallbacksReceived == channels.size) {
                                    fetchInProgress = false
                                    callbackWhenDoneFetchingAllChannels(details)
                                }
                            })
                }
            }


            // Returns all Details in the database (after fetching from YouTube) that belong to the given channels
            private fun fetchAllDetails(context: Context,
                                        channel: Channel,
                                        stopAtDate: DateTime?,
                                        setPercentageCallback: (totalVideos: kotlin.Int, currentVideoNumber: kotlin.Int) -> Unit,
                                        callback: (details: List<Detail>) -> Unit) {


                // Now that we have all details from the database, append the ones we find from YouTube
                YouTubeAPI.fetchAllDetails(channel, stopAtDate, setPercentageCallback, { newDetails: List<Detail> ->
                    run {
                        val dbHelper = DetailsOpenHelper(context.applicationContext)

                        // Get all Details from database that belong to the specified channel (not the entire list of channels)
                        val detailsFromDb = dbHelper.getAllDetailsFromDb(listOf(channel)).toMutableList()

                        // We got all the new Details from YouTube, so append them to the database.
                        // Remove duplicates before adding to the database.
                        val newDetailsMutable = newDetails.toMutableList()
                        for (detail in newDetails) {
                            if (detailsFromDb.contains(detail)) {
                                newDetailsMutable.remove(detail)
                            }
                        }

                        // Append to the database
                        dbHelper.addDetails(newDetailsMutable)

                        detailsFromDb.addAll(newDetailsMutable)

                        // Return to the original callback the combined list of unsorted Details
                        callback(detailsFromDb)
                    }
                })
            }
        }
    }







    class DetailsOpenHelper internal constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion != newVersion) {
                // Simplest implementation is to drop all old tables and recreate them
                db.execSQL("DROP TABLE IF EXISTS " + DETAILS_TABLE_NAME)
                onCreate(db)
            }
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DETAILS_TABLE_CREATE)
        }

        // Insert a Detail into the database
        fun addDetails(details: List<Detail>) {

            // Create and/or open the database for writing
            val db = writableDatabase

            // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
            // consistency of the database.
            db.beginTransaction()

            try {
                for(detail in details) {
                    val values = ContentValues()
                    values.put(KEY_CHANNELID, detail.channel.channelId)
                    values.put(KEY_VIDEOID, detail.videoId)
                    values.put(KEY_TITLE, detail.title)
                    values.put(KEY_DESCRIPTION, detail.description)
                    values.put(KEY_THUMBNAIL, detail.thumbnail)
                    values.put(KEY_DATE_UPLOADED, detail.dateUploaded.toStringRfc3339())

                    // Notice how we haven't specified the primary key. SQLite auto increments the primary key column.
                    db.insertOrThrow(DETAILS_TABLE_NAME, null, values)
                }
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.d(TAG, "Error while trying to add Detail to database")
            } finally {
                db.endTransaction()
            }
        }

        // Get all Details from the database that belong to the given channel
        fun getAllDetailsFromDb(channels: List<Channel>): List<Detail> {
            val allDetails = mutableListOf<Detail>()

            // SELECT * FROM DETAILS WHERE ChannelId = $channel.id
            val DETAILS_SELECT_QUERY = "SELECT * FROM $DETAILS_TABLE_NAME WHERE $KEY_CHANNELID = ?"
            val db: SQLiteDatabase


            try {
                db = this.readableDatabase
            } catch (s: SQLException) {
                // We sometimes get an error opening the database.
                // Don't save the watched time. 's ok. Maybe next time.
                return listOf()
            }
            try {
                for (channel in channels) {
                    val cursor = db.rawQuery(DETAILS_SELECT_QUERY, arrayOf(channel.channelId))
                    try {
                        if (cursor.moveToFirst()) {
                            do {
                                val videoId = cursor.getString(cursor.getColumnIndex(KEY_VIDEOID))
                                val title = cursor.getString(cursor.getColumnIndex(KEY_TITLE))
                                val description = cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION))
                                val thumbnail = cursor.getString(cursor.getColumnIndex(KEY_THUMBNAIL))
                                val dateUploaded = cursor.getString(cursor.getColumnIndex(KEY_DATE_UPLOADED))
                                val dateRfc3339 = DateTime.parseRfc3339(dateUploaded)

                                val newDetail = Detail(channel, videoId, title, description, thumbnail, dateRfc3339)
                                allDetails.add(newDetail)
                            } while (cursor.moveToNext())
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Inner error while trying to get details from database")
                    } finally {
                        if (cursor != null && !cursor.isClosed) {
                            cursor.close()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error while trying to get details from database")
            } finally {
                db.close()
            }

            return allDetails
        }

        fun deleteAllDetails() {
            val db = writableDatabase
            db.beginTransaction()
            try {
                // Order of deletions is important when foreign key relationships exist.
                db.delete(DETAILS_TABLE_NAME, null, null)
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.d(TAG, "Error while trying to delete all details")
            } finally {
                db.endTransaction()
            }
        }
    }
}
