package com.mymikemiller.chronoplayer

import android.app.AlertDialog
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceActivity
import android.content.Intent
import android.preference.EditTextPreference
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast
import android.preference.Preference
import android.util.Log
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.mymikemiller.chronoplayer.util.CommitPlaylists
import com.mymikemiller.chronoplayer.util.VideoList
import com.mymikemiller.chronoplayer.yt.YouTubeAPI
import android.app.ProgressDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView


/**
 * The settings activity
 */
class PreferencesActivity : PreferenceActivity(),
        GoogleApiClient.ConnectionCallbacks  {

    companion object {
        val RC_SIGN_IN = 3 // The request code for google sign in
        val TAG = "PreferencesActivity"

        const val CHANNEL_SELECT = "com.mymikemiller.chronoplayer.CHANNEL_SELECT"
        const val CHANGE_PLAYLIST_NAME = "com.mymikemiller.chronoplayer.CHANGE_PLAYLIST_NAME"
        const val UNSKIP_ALL = "com.mymikemiller.chronoplayer.UNSKIP_ALL"
        const val WATCH_HISTORY = "com.mymikemiller.chronoplayer.WATCH_HISTORY"

        private var mYouTubeAPI: YouTubeAPI? = null
        private lateinit var mGoogleApiClient: GoogleApiClient

        private lateinit var mProgressTitle: TextView
        private lateinit var mProgressBar: ProgressBar
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentManager.beginTransaction().replace(android.R.id.content, MyPreferenceFragment()).commit()
    }

    // The user is now authenticated
    override fun onConnected(p0: Bundle?) {
        Toast.makeText(this, getString(R.string.connection_success),
                Toast.LENGTH_LONG).show()
    }

    override fun onConnectionSuspended(p0: Int) {
        Toast.makeText(this, getString(R.string.connection_suspended),
                Toast.LENGTH_LONG).show()
    }

    class MyPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)

            updateUI(isSignedIn())

            // Change the playlist name description text to mention the playlist name sent in
            val playlistName = activity.intent.extras.getString("playlistName")
            val changePlaylistNamePref: EditTextPreference = findPreference(
                    getString(R.string.pref_changePlaylistNameKey)) as EditTextPreference;
            changePlaylistNamePref.setSummary(playlistName);
            changePlaylistNamePref.setTitle(getString(R.string.changePlaylistNameTitle))

            // Also change the text in the EditText to match what was sent in
            changePlaylistNamePref.editText.setText(playlistName)


            // Configure sign-in to request youtube access
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestScopes(Scope(Scopes.PLUS_LOGIN))
                    .requestScopes(Scope(YouTubeAPI.YOUTUBE_SCOPE))
                    .requestEmail()
                    .build()

            // Build a GoogleApiClient with access to the Google Sign-In API and the
            // options specified by gso.
            mGoogleApiClient = GoogleApiClient.Builder(activity)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .addConnectionCallbacks(activity as PreferencesActivity).build()
            mGoogleApiClient.connect()

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

                signIn()

                true
            })

            val signOutButton = findPreference(getString(R.string.pref_signOutKey))
            signOutButton.setOnPreferenceClickListener({

                signOut()

                true
            })


            val commitPlaylistButton = findPreference(getString(R.string.pref_commitPlaylistKey))
            commitPlaylistButton.setOnPreferenceClickListener({

                commitPlaylist()

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
        }

        fun updateUI(signedIn: Boolean) {
            findPreference(getString(R.string.pref_signInKey)).isEnabled = !signedIn
            findPreference(getString(R.string.pref_signOutKey)).isEnabled = signedIn
            findPreference(getString(R.string.pref_commitPlaylistKey)).isEnabled = signedIn
        }

        // This happens as a result of signing in for the first time by selecting a user
        fun handleSignInResult(result: GoogleSignInResult) : Unit {
            Log.d(TAG, "handleSignInResult: " + result.isSuccess())
            if (result.isSuccess()) {

                Toast.makeText(activity, "Signed in successfully",
                        Toast.LENGTH_LONG).show()

                // Get the account from the sign in result
                val account: GoogleSignInAccount? = result.signInAccount

                if (account != null) {
                    // Initialize mYouTubeAPI because we're now authenticated and can call the
                    // authenticated calls
                    mYouTubeAPI = YouTubeAPI(activity, account.account!!)
                    updateUI(true)
                }
            } else {
                Toast.makeText(activity, "Failed to sign in",
                        Toast.LENGTH_LONG).show()

                // Clear mYouTubeApi so we don't try to use authenticated functions
                mYouTubeAPI = null
            }
        }


        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
            super.onActivityResult(requestCode, resultCode, data)

            if (requestCode == RC_SIGN_IN) {
                // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
                val result: GoogleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                handleSignInResult(result)
            }
        }


        fun isSignedIn(): Boolean {
            return mYouTubeAPI != null
        }

        fun signIn() {
            if (!isSignedIn()) {
                val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
                startActivityForResult(signInIntent, RC_SIGN_IN)

                // We don't call updateUI here because we need to wait for the activity's
                // result to see if we actually signed in
            }
        }

        fun signOut() {
            if (isSignedIn()) {
                Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback {
                    // Clear mYouTubeAPI so we don't try to make any authenticated calls
                    mYouTubeAPI = null

                    updateUI(false)
                }
            }
        }

        fun commitPlaylist() {
            if (isSignedIn()) {

                val channel = activity.intent.getSerializableExtra("channel") as Channel

                // We can't use the intent to pass in the list of details because it's too big.
                // Instead, get the list of details from the database
                val details = VideoList.getAllDetailsFromDb(activity, channel)
                val playlistName = CommitPlaylists.getCommitPlaylistTitle(activity, channel)

                val inflater =  activity.getSystemService (Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val view: View = inflater.inflate(R.layout.progress_dialog, null);

                mProgressTitle = view.findViewById<TextView>(R.id.progressTitle)
                mProgressBar = view.findViewById<ProgressBar>(R.id.progressBar)

                val builder: AlertDialog.Builder = AlertDialog.Builder(activity);
                builder.setView(view);
                val dialog = builder.create();

                dialog.show();

                mYouTubeAPI!!.addVideosToPlayList(playlistName, details, setPercentageOfVideosAdded)
            } else {
                Toast.makeText(activity, getString(R.string.not_signed_in),
                        Toast.LENGTH_LONG).show()
            }
        }

        val setPercentageOfVideosAdded: (kotlin.Int, kotlin.Int) -> Unit = { totalVideos, currentVideoNumber ->
            run {
                Log.d("progress", currentVideoNumber.toString() + "/" + totalVideos)

                activity.runOnUiThread({
                    mProgressBar.max = totalVideos
                    mProgressBar.setProgress(currentVideoNumber)

                    mProgressTitle.setText(getString(R.string.commitProgressTitle) + " (" + currentVideoNumber + "/" + totalVideos + ")")

                })
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            mGoogleApiClient.disconnect()
        }
    }
}