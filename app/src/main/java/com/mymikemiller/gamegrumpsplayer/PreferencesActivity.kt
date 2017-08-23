package com.mymikemiller.gamegrumpsplayer

import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceActivity
import android.preference.Preference
import android.content.SharedPreferences





/**
 * The settings activity
 */


class PreferencesActivity : PreferenceActivity() {
    companion object {
        const val KEY_PREF_PLAYLIST_ORDER = "playlistOrder"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content, MyPreferenceFragment()).commit()
    }

    class MyPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
        }
    }

}