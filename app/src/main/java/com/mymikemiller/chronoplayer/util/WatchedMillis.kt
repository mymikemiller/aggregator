package com.mymikemiller.chronoplayer.util

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.mymikemiller.chronoplayer.Detail
import java.sql.SQLException

/**
 *  Keeps track of how long you've watched each episode.
 */
class WatchedMillis {
    companion object {
        // Increment this when the table definition changes
        val DATABASE_VERSION: Int = 68
        val DATABASE_NAME: String = "WatchedMillis"
        val WATCHED_MILLIS_TABLE_NAME: String = "WatchedMillisTable"

        // VideoList columns
        val KEY_VIDEOID: String="Video_Id"
        val KEY_WATCHED_MILLIS: String = "Watched_Millis"

        private val WATCHED_MILLIS_TABLE_CREATE =
                "CREATE TABLE " + WATCHED_MILLIS_TABLE_NAME + " (" +
                        KEY_VIDEOID + " TEXT NOT NULL UNIQUE, " +
                        KEY_WATCHED_MILLIS + " INTEGER);"

        // This will return all the Details currently in the database, and will call
        // the databaseUpgradeCallback if the database had to be upgraded to a new version by
        // incrementing the DATABASE_VERSION above
        fun getWatchedMillis (context: Context, detail: Detail): Int{
            val dbHelper = WatchedMillisOpenHelper(context.applicationContext)
            return dbHelper.getWatchedMillis(detail)
        }

        fun addOrUpdateWatchedMillis(context:Context, detail: Detail, watchedMillis: Int) {
            val dbHelper = WatchedMillisOpenHelper(context.applicationContext)
            return dbHelper.addOrUpdateWatchedMillis(detail, watchedMillis)

        }
    }

    class WatchedMillisOpenHelper internal constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion != newVersion) {
                // Simplest implementation is to drop all old tables and recreate them
                db.execSQL("DROP TABLE IF EXISTS " + WATCHED_MILLIS_TABLE_NAME)
                onCreate(db)
            }
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(WATCHED_MILLIS_TABLE_CREATE)
        }

        fun getWatchedMillis(detail: Detail): Int {
            // If we don't find any rows, the watchedMillis should be 0
            var watchedMillis = 0

            // SELECT
            //  ISNULL(MAX(WatchedMillis),'John Doe')
            // FROM
            //  Users
            // WHERE
            //  Id = @UserId
            val SELECT_QUERY = "SELECT $KEY_WATCHED_MILLIS" +
                    " FROM $WATCHED_MILLIS_TABLE_NAME WHERE $KEY_VIDEOID='${detail.videoId}'"

            //val SELECT_QUERY = "SELECT * FROM $WATCHED_MILLIS_TABLE_NAME"
            val db: SQLiteDatabase
            try {
                db = this.readableDatabase
            } catch (s: SQLException) {
                // We sometimes get an error opening the database.
                // Don't get the watched time, just return 0.
                return 0
            }

            val cursor = db.rawQuery(SELECT_QUERY, null)
            try {
                if (cursor.moveToFirst()) {
                    watchedMillis = cursor.getInt(cursor.getColumnIndex(KEY_WATCHED_MILLIS))
                }
            } catch (e: Exception) {
                Log.d(ContentValues.TAG, "Error while trying to get watched millis from database")
            } finally {
                if (cursor != null && !cursor.isClosed) {
                    cursor.close()
                }

                db.close()
            }

            return watchedMillis
        }

        // Add or update the watched time.
        fun addOrUpdateWatchedMillis(detail: Detail, watchedMillis: Int) {
            val db: SQLiteDatabase
            try {
                db = this.writableDatabase
            } catch (s: SQLException) {
                // We sometimes get an error opening the database.
                // Don't save the watched time. 's ok. Maybe next time.
                return
            }

            val initialValues = ContentValues()
            initialValues.put(KEY_VIDEOID, detail.videoId) // the execution is different if _id is 2
            initialValues.put(KEY_WATCHED_MILLIS, watchedMillis)

            // This is some magic that will insert, and if it can't, it'll update
            val id = db.insertWithOnConflict(WATCHED_MILLIS_TABLE_NAME, null, initialValues, SQLiteDatabase.CONFLICT_IGNORE)
            if (id == (-1).toLong()) {
                db.update(WATCHED_MILLIS_TABLE_NAME, initialValues, "$KEY_VIDEOID=?", arrayOf(detail.videoId))  // number 1 is the _id here, update to variable for your code
            }

            db.close()
        }
    }
}
