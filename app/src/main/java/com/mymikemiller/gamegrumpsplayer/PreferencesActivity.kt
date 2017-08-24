package com.mymikemiller.gamegrumpsplayer

import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceActivity

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

    class MyPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
        }
    }

}