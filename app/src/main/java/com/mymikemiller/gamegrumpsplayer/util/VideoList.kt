package com.mymikemiller.gamegrumpsplayer.util

import android.content.Context
import com.mymikemiller.gamegrumpsplayer.Details
import com.mymikemiller.gamegrumpsplayer.yt.YouTubeAPI
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues.TAG
import android.content.ContentValues
import android.util.Log


/**
 * VideoList stores video Details in a local SQL database and manages talking to the YouTubeAPI to fetch videos from YouTube when necessary
 */

class VideoList {
    companion object {
        val DATABASE_VERSION: Int = 3 // Increment this when the table definition changes
        val DATABASE_NAME: String = "VideoList"
        val DETAILS_TABLE_NAME: String = "VideoListTable"

        // VideoList columns
        val KEY_TITLE: String = "Title"
        val KEY_DESCRIPTION: String = "Description"

        private val DETAILS_TABLE_CREATE =
                "CREATE TABLE " + DETAILS_TABLE_NAME + " (" +
                        KEY_TITLE + " TEXT, " +
                        KEY_DESCRIPTION + " TEXT);"

        fun fetchAllDetailsByChannelId(context: Context,
                                       channelId: String,
                                       setPercentageCallback: (totalVideos: kotlin.Int, currentVideoNumber: kotlin.Int) -> Unit,
                                       callback: (details: List<Details>) -> Unit) {
            val openHelper = DetailsOpenHelper(context.applicationContext)

            //TODO: move this to a background process because this may be creating a table, which is expensive
            val writableDatabase = openHelper.writableDatabase

            // Create sample data
            val sampleDetails = Details("TestId", "testTitle", "testDescription", "testThumbnail")

            // Add sample post to the database
            openHelper.addDetails(sampleDetails)

            // Get all posts from database
            val allDetails = openHelper.getAllDetails()
            for (details in allDetails) {
                println("details from database: $details")
            }

            YouTubeAPI.fetchAllDetailsByChannelId(channelId, setPercentageCallback, callback)
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

        // Insert a post into the database
        fun addDetails(details: Details) {
            // Create and/or open the database for writing
            val db = writableDatabase

            // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
            // consistency of the database.
            db.beginTransaction()
            try {
                val values = ContentValues()
                values.put(KEY_TITLE, details.fullVideoTitle)
                values.put(KEY_DESCRIPTION, details.fullVideoDescription)

                // Notice how we haven't specified the primary key. SQLite auto increments the primary key column.
                db.insertOrThrow(DETAILS_TABLE_NAME, null, values)
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.d(TAG, "Error while trying to add post to database")
            } finally {
                db.endTransaction()
            }
        }
        fun getAllDetails(): List<Details> {
            val allDetails = mutableListOf<Details>()

            // SELECT * FROM DETAILS
            val POSTS_SELECT_QUERY = "SELECT * FROM $DETAILS_TABLE_NAME"

            // "getReadableDatabase()" and "getWriteableDatabase()" return the same object (except under low
            // disk space scenarios)
            val db = readableDatabase
            val cursor = db.rawQuery(POSTS_SELECT_QUERY, null)
            try {
                if (cursor.moveToFirst()) {
                    do {
                        val title = cursor.getString(cursor.getColumnIndex(KEY_TITLE))
                        val description = cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION))

                        val newDetails = Details(title, description, "", "")
                        allDetails.add(newDetails)
                    } while (cursor.moveToNext())
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error while trying to get posts from database")
            } finally {
                if (cursor != null && !cursor.isClosed) {
                    cursor.close()
                }
            }
            return allDetails
        }

        // Update a Details. We probably won't use this. If we need to update other columns we'll have to create more methods like this.
        fun updateTitle(details: Details): Int {
            val db = this.writableDatabase

            val values = ContentValues()
            values.put(KEY_TITLE, details.title)

            // Updating profile picture url for user with that userName
            return db.update(DETAILS_TABLE_NAME, values, KEY_TITLE + " = ?",
                    arrayOf(details.title))
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
