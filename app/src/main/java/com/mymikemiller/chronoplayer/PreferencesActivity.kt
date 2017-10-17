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
import com.mymikemiller.chronoplayer.util.PlaylistChannels
import com.mymikemiller.chronoplayer.util.VideoList
import com.mymikemiller.chronoplayer.yt.YouTubeAPI
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.mymikemiller.chronoplayer.util.PlaylistManipulator
import com.mymikemiller.chronoplayer.util.RemovePrevious


/**
 * The settings activity
 */
class PreferencesActivity : PreferenceActivity(),
        GoogleApiClient.ConnectionCallbacks  {

    companion object {
        val RC_SIGN_IN = 3 // The request code for google sign in
        val TAG = "PreferencesActivity"
        val EXTRA_PLAYLIST_TITLE = "playlistTitle"
        val APP_SHARED_PREFERENCES = "CHRONOPLAYER_SHARED_PREFERENCES"
        val PLAYLIST_SHARED_PREF_NAME = "PlaylistPrefs"

        const val MANAGE_CHANNELS = "com.mymikemiller.chronoplayer.MANAGE_CHANNELS"
        const val CHANGE_PLAYLIST_TITLE = "com.mymikemiller.chronoplayer.CHANGE_PLAYLIST_TITLE"
        const val SHOW_ALL = "com.mymikemiller.chronoplayer.SHOW_ALL"
        const val WATCH_HISTORY = "com.mymikemiller.chronoplayer.WATCH_HISTORY"

        private lateinit var mGoogleApiClient: GoogleApiClient

        private lateinit var mProgressTitle: TextView
        private lateinit var mProgressBar: ProgressBar
        private lateinit var mCommitProgressDialog: AlertDialog
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
            val playlistTitle = activity.intent.extras.getString(EXTRA_PLAYLIST_TITLE)
            val changePlaylistTitle: Preference = findPreference(
                    getString(R.string.pref_changePlaylistTitleKey)) as Preference;
            changePlaylistTitle.setSummary(playlistTitle);
            changePlaylistTitle.setTitle(getString(R.string.changePlaylistTitleTitle))

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

            val manageChannelsButton = findPreference(getString(R.string.pref_manageChannelsKey))
            manageChannelsButton.setOnPreferenceClickListener({

                val intent = Intent()
                intent.action = MANAGE_CHANNELS
                LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)

                activity.finish()

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

            val changePlaylistButton = findPreference(getString(R.string.pref_changePlaylistTitleKey))
            changePlaylistButton.setOnPreferenceClickListener({

                val intent = Intent()
                intent.action = CHANGE_PLAYLIST_TITLE
                LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)

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

            val showAllVideosButton = findPreference(getString(R.string.pref_showAllVideosKey))
            showAllVideosButton.setOnPreferenceClickListener({

                val intent = Intent()
                intent.action = SHOW_ALL
                LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)

                Toast.makeText(getActivity(), getString(R.string.allVideosShown),
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

                // Get the account from the sign in result
                val account: GoogleSignInAccount? = result.signInAccount

                if (account != null) {
                    // Initialize mYouTubeAPI because we're now authenticated and can call the
                    // authenticated calls
                    YouTubeAPI.authenticate(activity, account.account!!)
                    updateUI(true)
                }
            } else {
                Toast.makeText(activity, "Failed to sign in",
                        Toast.LENGTH_LONG).show()

                // Clear mYouTubeApi so we don't try to use authenticated functions
                YouTubeAPI.unAuthenticate()
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
            return YouTubeAPI.isAuthenticated()
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
                    YouTubeAPI.unAuthenticate()

                    updateUI(false)
                }
            }
        }

        fun commitPlaylist() {
            if (isSignedIn()) {

                val playlistTitle = activity.intent.getSerializableExtra(PreferencesActivity.EXTRA_PLAYLIST_TITLE) as String

                val channels = PlaylistChannels.getChannels(activity, playlistTitle)

                // We can't use the intent to pass in the list of details because it's too big.
                // Instead, get the list of details from the database
                // TODO: Make this work using the playlistTitle to find the remove before date
                val details = RemovePrevious.filterOutRemoved(activity, playlistTitle,
                        PlaylistManipulator.orderByDate(
                                VideoList.getAllDetailsFromDb(activity, channels))).asReversed()

                val prefs: SharedPreferences = activity.getSharedPreferences(APP_SHARED_PREFERENCES, Context.MODE_PRIVATE);
                var playlistName = prefs.getString(PLAYLIST_SHARED_PREF_NAME, "");

                playlistName = "gg"

                if (playlistName.isBlank()) {
                    Toast.makeText(activity, "No playlist title set", Toast.LENGTH_LONG)
                    return
                }

                // Get the last video in the user's playlist so we can start adding after that video
                YouTubeAPI.getDetailsToCommit(playlistName, details, { detailsToCommit ->
                    if (detailsToCommit.isEmpty()) {
                        activity.runOnUiThread({
                            Toast.makeText(activity, getString(R.string.noVideosToCommit),
                                    Toast.LENGTH_LONG).show()
                        })
                    } else {
                        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                        val view: View = inflater.inflate(R.layout.progress_dialog, null);

                        mProgressTitle = view.findViewById<TextView>(R.id.progressTitle)
                        mProgressBar = view.findViewById<ProgressBar>(R.id.progressBar)

                        activity.runOnUiThread({
                            val builder: AlertDialog.Builder = AlertDialog.Builder(activity);
                            builder.setView(view);
                            mCommitProgressDialog = builder.create();
                            mCommitProgressDialog.setOnDismissListener({
                                YouTubeAPI.cancelCommit()
                            })

                            mCommitProgressDialog.setCanceledOnTouchOutside(false)
                            mCommitProgressDialog.show();
                        })

                        YouTubeAPI.addVideosToPlayList(playlistName, detailsToCommit, setPercentageOfVideosAdded)
                    }
                })
            } else {
                Toast.makeText(activity, getString(R.string.not_signed_in),
                        Toast.LENGTH_LONG).show()
            }
        }

        val setPercentageOfVideosAdded: (kotlin.Int, kotlin.Int) -> Unit = { totalVideos, currentVideoNumber ->
            run {

                activity.runOnUiThread({
                    mProgressBar.max = totalVideos
                    mProgressBar.setProgress(currentVideoNumber)

                    mProgressTitle.setText(getString(R.string.commitProgressTitle) + " (" + currentVideoNumber + "/" + totalVideos + ")")

                    if (currentVideoNumber == totalVideos) {
                        // After a short delay (so the user can see the progress at 100%), dismiss the dialog
                        Handler().postDelayed({
                            mCommitProgressDialog.dismiss()
                        }, 1000)
                    }
                })
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            YouTubeAPI.cancelCommit()
            mGoogleApiClient.disconnect()
        }
    }
}