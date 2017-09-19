package com.mymikemiller.chronoplayer

import android.app.Activity
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceActivity
import android.preference.ListPreference
import android.content.SharedPreferences
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast


/**
 * The settings activity
 */
class PreferencesActivity : PreferenceActivity() {
    companion object {
        const val UNSKIP_ALL = "com.example.mymikemiller.UNSKIP_ALL"
        const val WATCH_HISTORY = "com.example.mymikemiller.WATCH_HISTORY"
        const val CHANNEL_SELECT = "com.example.mymikemiller.CHANNEL_SELECT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        fragmentManager.beginTransaction().replace(android.R.id.content, MyPreferenceFragment()).commit()
    }

    class MyPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)

            val unskipAllVideosButton = findPreference(getString(R.string.pref_unskipAllVideosKey))
            unskipAllVideosButton.setOnPreferenceClickListener({

                val intent = Intent()
                intent.action = UNSKIP_ALL
                LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)

                Toast.makeText(getActivity(), getString(R.string.videosUnskipped),
                        Toast.LENGTH_SHORT).show()

                true
            })

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

            val channelSelectButton = findPreference(getString(R.string.pref_channelSelectKey))
            channelSelectButton.setOnPreferenceClickListener({

                // Close the preferences pane so we go directly back to the main activity when we
                // select a video to play
                activity.finish()

                val intent = Intent()
                intent.action = CHANNEL_SELECT
                LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)

                true
            })
        }
    }
}