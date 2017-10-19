package com.mymikemiller.chronoplayer.util

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.mymikemiller.chronoplayer.Detail
import com.google.api.client.util.DateTime
import java.sql.SQLException

/**
 * This class uses the database to keep track of which date the user has specified they'd like remove videos before
 */
class RemovePrevious {
    companion object {

        val DATABASE_VERSION: Int = 5
        val DATABASE_NAME: String = "RemovePrevious"
        val REMOVE_BEFORE_DATE_TABLE_NAME: String = "RemovePreviousTable"

        val KEY_PLAYLIST_TITLE: String = "PlaylistTitle"
        val KEY_REMOVE_BEFORE_DATE: String="RemoveBeforeDate"

        private val REMOVE_BEFORE_DATE_TABLE_CREATE =
                "CREATE TABLE " + REMOVE_BEFORE_DATE_TABLE_NAME + " (" +
                        KEY_PLAYLIST_TITLE + " TEXT NOT NULL, " +
                        KEY_REMOVE_BEFORE_DATE + " TEXT NOT NULL UNIQUE);"

        fun filterOutRemoved(context: Context, playlistTitle: String, details: List<Detail>) : List<Detail> {
            Log.e(DATABASE_NAME, "filterOutRemoved is not implemented yet")
            val dbHelper = RemoveBeforeDateOpenHelper(context.applicationContext)
            val dateToRemoveBefore = dbHelper.getRemoveBeforeDateFromDb(playlistTitle)
            var newDetails = details
            if (dateToRemoveBefore != null) {
                newDetails = details.filter { it -> it.dateUploaded.value >= dateToRemoveBefore.value}
            }
            return newDetails
        }

        fun setRemovedBeforeDate(context: Context, playlistTitle: String, date: DateTime) {
            val dbHelper = RemoveBeforeDateOpenHelper(context.applicationContext)
            dbHelper.setRemoveBeforeDate(playlistTitle, date)
        }

        fun getRemoveBeforeDate(context: Context, playlistTitle: String
        ): DateTime? {
            val dbHelper = RemoveBeforeDateOpenHelper(context.applicationContext)
            val removeBeforeDate = dbHelper.getRemoveBeforeDateFromDb(playlistTitle)
            return removeBeforeDate
        }

        fun unRemove(context: Context, playlistTitle: String) {
            val dbHelper = RemoveBeforeDateOpenHelper(context.applicationContext)
            dbHelper.unRemove(playlistTitle)
        }
    }

    class RemoveBeforeDateOpenHelper internal constructor(context: Context) : SQLiteOpenHelper(context, RemovePrevious.DATABASE_NAME, null, RemovePrevious.DATABASE_VERSION) {

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion != newVersion) {
                // Simplest implementation is to drop all old tables and recreate them
                db.execSQL("DROP TABLE IF EXISTS " + RemovePrevious.REMOVE_BEFORE_DATE_TABLE_NAME)
                onCreate(db)
            }
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(RemovePrevious.REMOVE_BEFORE_DATE_TABLE_CREATE)
        }

        fun setRemoveBeforeDate(playlistTitle: String, date: DateTime) {

            // First remove the current removeBeforeDate
            unRemove(playlistTitle)

            // Create and/or open the database for writing
            val db = writableDatabase

            // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
            // consistency of the database.
            db.beginTransaction()

            try {
                val values = ContentValues()
                values.put(RemovePrevious.KEY_PLAYLIST_TITLE, playlistTitle)
                values.put(RemovePrevious.KEY_REMOVE_BEFORE_DATE, date.toStringRfc3339())

                // Notice how we haven't specified the primary key. SQLite auto increments the primary key column.
                db.insertOrThrow(REMOVE_BEFORE_DATE_TABLE_NAME, null, values)

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "Error while trying to add Remove Before Date to database")
            } finally {
                db.endTransaction()
            }
        }

        fun getRemoveBeforeDateFromDb(playlistTitle: String): DateTime? {
            var date: DateTime? = null

            val REMOVED_BEFORE_SELECT_QUERY = "SELECT * FROM ${RemovePrevious.REMOVE_BEFORE_DATE_TABLE_NAME} " +
                    "WHERE $KEY_PLAYLIST_TITLE = ? LIMIT 1"
            val db: SQLiteDatabase
            try {
                db = this.readableDatabase
            } catch (s: SQLException) {
                // We sometimes get an error opening the database.
                return date
            }

            val cursor = db.rawQuery(REMOVED_BEFORE_SELECT_QUERY, arrayOf(playlistTitle))
            try {
                if (cursor.moveToFirst()) {
                    date = DateTime.parseRfc3339(cursor.getString(cursor.getColumnIndex(RemovePrevious.KEY_REMOVE_BEFORE_DATE)))
                }
            } catch (e: Exception) {
                Log.d(ContentValues.TAG, "Error while trying to get Remove Before Date from database")
            } finally {
                if (cursor != null && !cursor.isClosed) {
                    cursor.close()
                }
                db.close()
            }

            return date
        }


        fun unRemove(playlistTitle: String) {
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
                db.delete(RemovePrevious.REMOVE_BEFORE_DATE_TABLE_NAME, KEY_PLAYLIST_TITLE + "= ?", arrayOf(playlistTitle))
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.d(ContentValues.TAG, "Error while trying to unRemove")
            } finally {
                db.endTransaction()
                db.close()
            }
        }
    }
}