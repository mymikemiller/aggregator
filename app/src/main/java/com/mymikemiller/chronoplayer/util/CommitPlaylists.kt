package com.mymikemiller.chronoplayer.util

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.mymikemiller.chronoplayer.Channel
import java.sql.SQLException

/**
 *  Keeps track of which playlist the user wants to commit to YouTube for the given channel
 */
class CommitPlaylists {
    companion object {
        // Increment this when the table definition changes
        val DATABASE_VERSION: Int = 2
        val DATABASE_NAME: String = "CommitPlaylists"
        val COMMIT_PLAYLISTS_TABLE_NAME: String = "CommitPlaylistsTable"

        // VideoList columns
        val KEY_CHANNEL_ID: String="Channel_Id"
        val PLAYLIST_TITLE: String = "Commit_Playlists"

        private val COMMIT_PLAYLISTS_TABLE_CREATE =
                "CREATE TABLE " + COMMIT_PLAYLISTS_TABLE_NAME + " (" +
                        KEY_CHANNEL_ID + " TEXT NOT NULL UNIQUE, " +
                        PLAYLIST_TITLE + " INTEGER);"

        // This will return the title of the palylist the user wants to commit to for
        // the specified channel
        fun getCommitPlaylistTitle (context: Context, channel: Channel): String{
            val dbHelper = CommitPlaylistsOpenHelper(context.applicationContext)
            return dbHelper.getCommitPlaylistTitle(channel)
        }

        fun addOrUpdateCommitPlaylistTitle(context: Context, channel: Channel, title: String) {
            val dbHelper = CommitPlaylistsOpenHelper(context.applicationContext)
            return dbHelper.addOrUpdateCommitPlaylistTitle(channel, title)

        }
    }

    class CommitPlaylistsOpenHelper internal constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion != newVersion) {
                // Simplest implementation is to drop all old tables and recreate them
                db.execSQL("DROP TABLE IF EXISTS " + COMMIT_PLAYLISTS_TABLE_NAME)
                onCreate(db)
            }
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(COMMIT_PLAYLISTS_TABLE_CREATE)
        }

        fun getCommitPlaylistTitle(channel: Channel): String {
            // If we don't find any rows, CommitPlaylists should be "" to show that we didn't find any
            var CommitPlaylistTitle = ""

            // SELECT
            //  ISNULL(MAX(CommitPlaylists), VideoId)
            // FROM
            //  Users
            // WHERE
            //  Id = @UserId
            val SELECT_QUERY = "SELECT $PLAYLIST_TITLE" +
                    " FROM $COMMIT_PLAYLISTS_TABLE_NAME WHERE $KEY_CHANNEL_ID='${channel.channelId}'"

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
                    CommitPlaylistTitle = cursor.getString(cursor.getColumnIndex(PLAYLIST_TITLE))
                }
            } catch (e: Exception) {
                Log.d(ContentValues.TAG, "Error while trying to get watched millis from database")
            } finally {
                if (cursor != null && !cursor.isClosed) {
                    cursor.close()
                }

                db.close()
            }

            return CommitPlaylistTitle
        }

        // Add or update the watched time.
        fun addOrUpdateCommitPlaylistTitle(channel: Channel, title: String) {
            val db: SQLiteDatabase
            try {
                db = this.writableDatabase
            } catch (s: SQLException) {
                // We sometimes get an error opening the database.
                return
            }

            val initialValues = ContentValues()
            initialValues.put(KEY_CHANNEL_ID, channel.channelId)
            initialValues.put(PLAYLIST_TITLE, title)

            //insertWithOnConflict returns the row ID of the newly inserted row OR -1 if an error occured (such as it's already in there_
            // This is some magic that will insert, and if it can't, it'll update
            val id = db.insertWithOnConflict(COMMIT_PLAYLISTS_TABLE_NAME, null, initialValues, SQLiteDatabase.CONFLICT_IGNORE)
            if (id == (-1).toLong()) {
                db.update(COMMIT_PLAYLISTS_TABLE_NAME, initialValues, "$KEY_CHANNEL_ID =?", arrayOf(channel.channelId))
            }

            db.close()
        }
    }
}