package com.mymikemiller.gamegrumpsplayer.util

import android.content.Context
import com.mymikemiller.gamegrumpsplayer.Detail
import com.mymikemiller.gamegrumpsplayer.yt.YouTubeAPI
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues.TAG
import android.content.ContentValues
import android.util.Log
import com.google.api.client.util.DateTime
import com.mymikemiller.gamegrumpsplayer.R
import java.sql.SQLException

/**
 * VideoList stores video Details in a local SQL database and manages talking to the YouTubeAPI to fetch videos from YouTube when necessary
 */
class VideoList {
    companion object {
        // Increment this when the table definition changes
        val DATABASE_VERSION: Int = 65
        val DATABASE_NAME: String = "VideoList"
        val DETAILS_TABLE_NAME: String = "VideoListTable"

        // VideoList columns
        val KEY_VIDEOID: String="Video_Id"
        val KEY_TITLE: String = "Title"
        val KEY_DESCRIPTION: String = "Description"
        val KEY_THUMBNAIL: String = "Thumbnail"
        val KEY_DATE_UPLOADED: String = "Date_Uploaded"

        private val DETAILS_TABLE_CREATE =
                "CREATE TABLE " + DETAILS_TABLE_NAME + " (" +
                        KEY_VIDEOID + " TEXT NOT NULL UNIQUE, " +
                        KEY_TITLE + " TEXT, " +
                        KEY_DESCRIPTION + " TEXT, " +
                        KEY_THUMBNAIL + " TEXT, " +
                        KEY_DATE_UPLOADED + " TEXT);"

        // This will return the number of Details currently in the database, and will call
        // the databaseUpgradeCallback if the database had to be upgraded to a new version by
        // incrementing the DATABASE_VERSION above
        fun getNumDetailsInDatabase(context: Context, databaseUpgradedCallback: () -> Unit) : Int {
            val dbHelper = DetailsOpenHelper(context.applicationContext, databaseUpgradedCallback)
            return dbHelper.getAllDetailsFromDb().size
        }

        // This will return all the Details currently in the database, and will call
        // the databaseUpgradeCallback if the database had to be upgraded to a new version by
        // incrementing the DATABASE_VERSION above. The returned details are in an arbitrary order.
        fun getAllDetailsFromDatabase(context: Context,
                                       databaseUpgradedCallback: () -> Unit): List<Detail>{
            val dbHelper = DetailsOpenHelper(context.applicationContext, databaseUpgradedCallback)

            return dbHelper.getAllDetailsFromDb()
        }

        fun fetchAllDetailsByChannelId(context: Context,
                                       databaseUpgradedCallback: () -> Unit,
                                       channelId: String,
                                       stopAtDetail: Detail?,
                                       setPercentageCallback: (totalVideos: kotlin.Int, currentVideoNumber: kotlin.Int) -> Unit,
                                       callback: (details: List<Detail>) -> Unit) {


            // Now that we have all details from the database, append the ones we find from YouTube
            YouTubeAPI.fetchAllDetailsByChannelId(channelId, stopAtDetail, setPercentageCallback, {newDetails: List<Detail> ->
                run {
                    val dbHelper = DetailsOpenHelper(context.applicationContext, databaseUpgradedCallback)

                    // Get all Details from database
                    val detailsFromDb = dbHelper.getAllDetailsFromDb().toMutableList()

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


        fun getDetailFromVideoId(context: Context, videoId: String) : Detail? {
            val details = getAllDetailsFromDatabase(context,
                    {})

            var returnDetail:Detail? = null
            for(detail in details) {
                if (detail.videoId == videoId) returnDetail = detail
            }
            return returnDetail
        }
    }

    class DetailsOpenHelper internal constructor(context: Context, val databaseUpgradedCallback: () -> Unit) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion != newVersion) {
                // Simplest implementation is to drop all old tables and recreate them
                db.execSQL("DROP TABLE IF EXISTS " + DETAILS_TABLE_NAME)
                onCreate(db)
                databaseUpgradedCallback()
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
                    values.put(KEY_VIDEOID, detail.videoId)
                    values.put(KEY_TITLE, detail.fullVideoTitle)
                    values.put(KEY_DESCRIPTION, detail.fullVideoDescription)
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

        fun getAllDetailsFromDb(): List<Detail> {
            val allDetails = mutableListOf<Detail>()

            // SELECT * FROM DETAILS
            val DETAILS_SELECT_QUERY = "SELECT * FROM $DETAILS_TABLE_NAME"
            val db: SQLiteDatabase

            try {
                db = this.readableDatabase
            } catch (s: SQLException) {
                // We sometimes get an error opening the database.
                // Don't save the watched time. 's ok. Maybe next time.
                return listOf()
            }

            val cursor = db.rawQuery(DETAILS_SELECT_QUERY, null)
            try {
                if (cursor.moveToFirst()) {
                    do {
                        val videoId = cursor.getString(cursor.getColumnIndex(KEY_VIDEOID))
                        val title = cursor.getString(cursor.getColumnIndex(KEY_TITLE))
                        val description = cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION))
                        val thumbnail = cursor.getString(cursor.getColumnIndex(KEY_THUMBNAIL))
                        val dateUploaded = cursor.getString(cursor.getColumnIndex(KEY_DATE_UPLOADED))
                        val dateRfc3339 = DateTime.parseRfc3339(dateUploaded)

                        val newDetail = Detail(videoId, title, description, thumbnail, dateRfc3339)
                        allDetails.add(newDetail)
                    } while (cursor.moveToNext())
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error while trying to get details from database")
            } finally {
                if (cursor != null && !cursor.isClosed) {
                    cursor.close()
                }
                db.close()
            }

            return allDetails
        }

        // Update a Detail. We probably won't use this. If we need to update other columns we'll have to create more methods like this.
        fun updateTitle(detail: Detail): Int {
            val db = this.writableDatabase

            val values = ContentValues()
            values.put(KEY_TITLE, detail.title)

            // Updating profile picture url for user with that userName
            return db.update(DETAILS_TABLE_NAME, values, KEY_TITLE + " = ?",
                    arrayOf(detail.title))
        }


        fun deleteAllDetails() {
            val db = writableDatabase
            db.beginTransaction()
            try {
                // Order of deletions is important when foreign key relationships exist.
                db.delete(DETAILS_TABLE_NAME, null, null)
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.d(TAG, "Error while trying to delete all posts and users")
            } finally {
                db.endTransaction()
            }
        }
    }
}
