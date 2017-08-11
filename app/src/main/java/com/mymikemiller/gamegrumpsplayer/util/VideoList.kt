package com.mymikemiller.gamegrumpsplayer.util

import android.content.Context
import com.mymikemiller.gamegrumpsplayer.Detail
import com.mymikemiller.gamegrumpsplayer.yt.YouTubeAPI
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues.TAG
import android.content.ContentValues
import android.util.Log


/**
 * VideoList stores video Detail in a local SQL database and manages talking to the YouTubeAPI to fetch videos from YouTube when necessary
 */

class VideoList {
    companion object {
        val DATABASE_VERSION: Int = 9 // Increment this when the table definition changes
        val DATABASE_NAME: String = "VideoList"
        val DETAILS_TABLE_NAME: String = "VideoListTable"

        // VideoList columns
        val KEY_VIDEOID: String="Video_Id"
        val KEY_TITLE: String = "Title"
        val KEY_DESCRIPTION: String = "Description"
        val KEY_THUMBNAIL: String = "Thumbnail"

        private val DETAILS_TABLE_CREATE =
                "CREATE TABLE " + DETAILS_TABLE_NAME + " (" +
                        KEY_VIDEOID + " TEXT, " +
                        KEY_TITLE + " TEXT, " +
                        KEY_DESCRIPTION + " TEXT, " +
                        KEY_THUMBNAIL + " TEXT);"

        fun fetchAllDetailsByChannelId(context: Context,
                                       channelId: String,
                                       setPercentageCallback: (totalVideos: kotlin.Int, currentVideoNumber: kotlin.Int) -> Unit,
                                       callback: (details: List<Detail>) -> Unit) {
            val openHelper = DetailsOpenHelper(context.applicationContext)

            //TODO: move this to a background process because this may be creating a table, which is expensive
//            val writableDatabase = openHelper.writableDatabase

            // Create sample data
            val sampleDetail = Detail("TestId", "testTitle", "testDescription", "testThumbnail")

            // Add sample post to the database

            // Create and/or open the database for writing
            val db = openHelper.writableDatabase

            // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
            // consistency of the database.
            db.beginTransaction()
            openHelper.addDetail(db, sampleDetail)
            db.endTransaction()

            // Get all posts from database
            val allDetails = openHelper.getAllDetails()
            for (detail in allDetails) {
                println("detail from database: $detail")
            }

            // Figure out which Detail we can stop at when we fetch from youtube
            val lastDetail = if (allDetails.isNotEmpty()) allDetails[allDetails.size - 1] else null

            // Now that we have all details from the database, append the ones we find from YouTube
            YouTubeAPI.fetchAllDetailsByChannelId(channelId, lastDetail, setPercentageCallback, {details: List<Detail> ->
                run {
                    println(details)

                    // We got all the new Details from YouTube, so append them to the database

                    // Create and/or open the database for writing
                    val db = openHelper.writableDatabase

                    // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
                    // consistency of the database.
                    db.beginTransaction()

                    for (detail in details) {
                        println("a detail from youtube: $detail")
                        openHelper.addDetail(db, detail)
                    }

                    db.endTransaction()

                    allDetails.addAll(details)

                    // Return to the original callback the combined list of all Details
                    callback(allDetails)
                }
            })
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
        fun addDetail(db: SQLiteDatabase, detail: Detail) {
            try {
                val values = ContentValues()
                values.put(KEY_VIDEOID, detail.videoId)
                values.put(KEY_TITLE, detail.fullVideoTitle)
                values.put(KEY_DESCRIPTION, detail.fullVideoDescription)
                values.put(KEY_THUMBNAIL, detail.thumbnail)

                // Notice how we haven't specified the primary key. SQLite auto increments the primary key column.
                db.insertOrThrow(DETAILS_TABLE_NAME, null, values)
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.d(TAG, "Error while trying to add post to database")
            } finally {
                val allDetails = getAllDetails()
                println(allDetails)
            }
        }

        fun getAllDetails(): MutableList<Detail> {
            val allDetails = mutableListOf<Detail>()

            // SELECT * FROM DETAILS
            val POSTS_SELECT_QUERY = "SELECT * FROM $DETAILS_TABLE_NAME"

            // "getReadableDatabase()" and "getWriteableDatabase()" return the same object (except under low
            // disk space scenarios)
            val db = readableDatabase
            val cursor = db.rawQuery(POSTS_SELECT_QUERY, null)
            try {
                if (cursor.moveToFirst()) {
                    do {
                        val videoId = cursor.getString(cursor.getColumnIndex(KEY_VIDEOID))
                        val title = cursor.getString(cursor.getColumnIndex(KEY_TITLE))
                        val description = cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION))
                        val thumbnail = cursor.getString(cursor.getColumnIndex(KEY_THUMBNAIL))

                        val newDetail = Detail(videoId, title, description, thumbnail)
                        allDetails.add(newDetail)
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
