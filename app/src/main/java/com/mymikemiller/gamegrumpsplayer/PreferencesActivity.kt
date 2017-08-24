package com.mymikemiller.gamegrumpsplayer

import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceActivity
import android.preference.ListPreference
import android.preference.Preference
import android.content.SharedPreferences




/**
 * The settings activity
 */


class PreferencesActivity : PreferenceActivity() {
    companion object {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content, MyPreferenceFragment()).commit()
    }

    class MyPreferenceFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)

            val pref = findPreference(getString(R.string.pref_playlistOrderKey))
            pref.summary = (pref as ListPreference).entry
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            /* get preference */
            val preference = findPreference(key)

            /* update summary */
            if (activity != null) {
                if (key == getString(R.string.pref_playlistOrderKey)) {
                    preference.summary = (preference as ListPreference).entry
                }
            }
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }
    }

}