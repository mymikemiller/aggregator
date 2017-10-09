package com.mymikemiller.chronoplayer

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceActivity
import android.content.Intent
import android.content.IntentFilter
import android.preference.EditTextPreference
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast
import android.preference.Preference
import android.util.Log

/**
 * The settings activity
 */
class PreferencesActivity : PreferenceActivity() {

    companion object {
        const val CHANNEL_SELECT = "com.mymikemiller.chronoplayer.CHANNEL_SELECT"
        const val SIGN_IN = "com.mymikemiller.chronoplayer.SIGN_IN"
        const val SIGN_OUT = "com.mymikemiller.chronoplayer.SIGN_OUT"
        const val COMMIT_PLAYLIST = "com.mymikemiller.chronoplayer.COMMIT_PLAYLIST"
        const val CHANGE_PLAYLIST_NAME = "com.mymikemiller.chronoplayer.CHANGE_PLAYLIST_NAME"
        const val UNSKIP_ALL = "com.mymikemiller.chronoplayer.UNSKIP_ALL"
        const val WATCH_HISTORY = "com.mymikemiller.chronoplayer.WATCH_HISTORY"
        const val USER_SIGNED_IN = "com.mymikemiller.chronoplayer.USER_SIGNED_IN"
        const val USER_SIGNED_OUT = "com.mymikemiller.chronoplayer.USER_SIGNED_OUT"

        private lateinit var mBroadcastReceiver: BroadcastReceiver
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentManager.beginTransaction().replace(android.R.id.content, MyPreferenceFragment()).commit()
    }

    class MyPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)

            val signedIn = activity.intent.getBooleanExtra("userIsSignedIn", false)
            handleSignInAndOut(signedIn)

            // Change the playlist name description text to mention the playlist name sent in
            val playlistName = activity.intent.extras.getString("playlistName")
            val changePlaylistNamePref: EditTextPreference = findPreference(
                    getString(R.string.pref_changePlaylistNameKey)) as EditTextPreference;
            changePlaylistNamePref.setSummary(playlistName);
            changePlaylistNamePref.setTitle(getString(R.string.changePlaylistNameTitle))

            // Also change the text in the EditText to match what was sent in
            changePlaylistNamePref.editText.setText(playlistName)

            val channelSelectButton = findPreference(getString(R.string.pref_channelSelectKey))
            channelSelectButton.setOnPreferenceClickListener({

                val intent = Intent()
                intent.action = CHANNEL_SELECT
                LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)

                //activity.finish()

                true
            })

            val signInButton = findPreference(getString(R.string.pref_signInKey))
            signInButton.setOnPreferenceClickListener({

                val intent = Intent()
                intent.action = SIGN_IN

                LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)

//                activity.finish()

                true
            })

            val signOutButton = findPreference(getString(R.string.pref_signOutKey))
            signOutButton.setOnPreferenceClickListener({

                val intent = Intent()
                intent.action = SIGN_OUT
                LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)

//                activity.finish()

                true
            })


            val commitPlaylistButton = findPreference(getString(R.string.pref_commitPlaylistKey))
            commitPlaylistButton.setOnPreferenceClickListener({

                val intent = Intent()
                intent.action = COMMIT_PLAYLIST
                LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)

                activity.finish()

                true
            })

            this.findPreference(getString(R.string.pref_changePlaylistNameKey))
                    .onPreferenceChangeListener = android.preference.Preference
                    .OnPreferenceChangeListener { preference, newValue ->

                        if (newValue.toString().length > 0) {

                            // Change the description text to mention the new playlist name
                            val customPref: Preference = findPreference(getString(R.string.pref_changePlaylistNameKey));
                            customPref.setSummary(newValue.toString());

                            // Get the playlist name from the EditText
                            val changePlaylistNamePref: EditTextPreference = findPreference(
                                    getString(R.string.pref_changePlaylistNameKey)) as EditTextPreference
                            val playlistName = changePlaylistNamePref.editText.text.toString()

                            val theIntent = Intent()
                            theIntent.action = CHANGE_PLAYLIST_NAME
                            theIntent.putExtra("playlistName", playlistName)
                            LocalBroadcastManager.getInstance(activity).sendBroadcast(theIntent)

                            return@OnPreferenceChangeListener true
                        }
                        return@OnPreferenceChangeListener false
                    }

            val watchHistoryButton = findPreference(getString(R.string.pref_watchHistoryKey))
            watchHistoryButton.setOnPreferenceClickListener({

                // Close the preferences pane so we go directly back to the main activity when we
                // select a video to play
                activity.finish()

                val intent = Intent()
                intent.action = WATCH_HISTORY
                LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)

                true
            })

            val unskipAllVideosButton = findPreference(getString(R.string.pref_unskipAllVideosKey))
            unskipAllVideosButton.setOnPreferenceClickListener({

                val intent = Intent()
                intent.action = UNSKIP_ALL
                LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)

                Toast.makeText(getActivity(), getString(R.string.videosUnskipped),
                        Toast.LENGTH_SHORT).show()


                true
            })

            // Register receivers so we know when the user signs in and out and can update the
            // UI accordingly
            mBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, theIntent: Intent?) {

                    when (theIntent?.action) {
                        USER_SIGNED_IN -> handleSignInAndOut(true)
                        USER_SIGNED_OUT -> handleSignInAndOut(false)
                    }
                }
            }
        }

        fun handleSignInAndOut(signedIn: Boolean) {
            findPreference(getString(R.string.pref_signInKey)).isEnabled = !signedIn
            findPreference(getString(R.string.pref_signOutKey)).isEnabled = signedIn
            findPreference(getString(R.string.pref_commitPlaylistKey)).isEnabled = signedIn
        }

        override fun onDestroy() {
            super.onDestroy()
            LocalBroadcastManager.getInstance(activity)
                    .unregisterReceiver(mBroadcastReceiver)
        }
    }
}