package com.mymikemiller.ytaggregator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import com.mymikemiller.ytaggregator.yt.YouTubeAPI
import android.widget.ArrayAdapter
import android.app.AlertDialog
import android.os.Handler
import android.text.InputType
import android.util.Log
import android.widget.EditText
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope

/**
 * Created by mikem on 10/14/2017.
 */
class PlaylistChooserActivity: AppCompatActivity(),
        GoogleApiClient.ConnectionCallbacks {


    lateinit var mListView: ListView
    val mListViewItems: MutableList<String> = mutableListOf()
    private lateinit var mGoogleApiClient: GoogleApiClient

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_chooser)

        // set up the action bar
        val myToolbar = findViewById<View>(R.id.playlist_chooser_toolbar) as Toolbar
        setSupportActionBar(myToolbar)

        mListView = findViewById<ListView>(R.id.listView)


        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mListViewItems)
        mListView.setAdapter(adapter)

        adapter.notifyDataSetChanged()

        mListView.onItemClickListener = object: AdapterView.OnItemClickListener {
            override fun onItemClick(l: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val playlistTitle = mListView.adapter.getItem(position) as String

                launchMainActivity(playlistTitle)
            }
        }

        // Configure sign-in to request youtube access
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(Scope(Scopes.PLUS_LOGIN))
                .requestScopes(Scope(YouTubeAPI.YOUTUBE_SCOPE))
                .requestEmail()
                .build()

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addConnectionCallbacks(this).build()
        mGoogleApiClient.connect()

        if (YouTubeAPI.isAuthenticated()) {
            showUserPlaylists()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu items for use in the action bar
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.show_my_playlists_menu, menu)
        return true;
    }
    @Override
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar items
        if (item.getItemId() == R.id.action_show_my_playlists) {

            signIn()

            return true;
        } else if (item.getItemId() == R.id.action_add_playlist) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Playlist Name")

            // Set up the input
            val input = EditText(this)
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            builder.setView(input)

            // Set up the buttons
            builder.setNegativeButton("Cancel", { dialog, which -> dialog.cancel() })
            builder.setPositiveButton("OK", { dialog, which ->
                run {
                    launchMainActivity(input.text.toString().trim())
                }
            })

            builder.show()

            // Pop up the keyboard
            val handler = Handler()
            handler.post(Runnable {
                input.requestFocus()
                val mgr = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                mgr.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            })

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Launch the main activity
    fun launchMainActivity(playlistTitle: String) {
        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        mainIntent.putExtra(getString(R.string.extraLaunchPlaylistTitle),
                playlistTitle)
        startActivity(mainIntent)
        finish()
    }

    override fun onConnected(p0: Bundle?) {
        // Do Nothing
    }

    override fun onConnectionSuspended(p0: Int) {
        // Do Nothing
    }

    fun signIn() {
        if (!YouTubeAPI.isAuthenticated()) {
            val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
            startActivityForResult(signInIntent, PreferencesActivity.RC_SIGN_IN)

            // We don't call updateUI here because we need to wait for the activity's
            // result to see if we actually signed in
        } else {
            showUserPlaylists()
        }
    }

    // This happens as a result of signing in for the first time by selecting a user
    fun handleSignInResult(result: GoogleSignInResult) : Unit {
        Log.d(PreferencesActivity.TAG, "handleSignInResult: " + result.isSuccess())
        if (result.isSuccess()) {

            Toast.makeText(this, "Signed in successfully",
                    Toast.LENGTH_SHORT).show()

            // Get the account from the sign in result
            val account: GoogleSignInAccount? = result.signInAccount

            if (account != null) {
                // Initialize mYouTubeAPI because we're now authenticated and can call the
                // authenticated calls
                YouTubeAPI.authenticate(this, account.account!!)
                showUserPlaylists()
            }
        } else {
            Toast.makeText(this, "Failed to sign in",
                    Toast.LENGTH_LONG).show()

            // Clear mYouTubeApi so we don't try to use authenticated functions
            YouTubeAPI.unAuthenticate()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PreferencesActivity.RC_SIGN_IN) {
            // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
            val result: GoogleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            handleSignInResult(result)
        }
    }

    fun showUserPlaylists() {
        if (YouTubeAPI.isAuthenticated()) {
            YouTubeAPI.getUserPlaylistTitles({ playlistTitles ->
                runOnUiThread({
                    for (playlistTitle in playlistTitles) {
                        mListViewItems.add(playlistTitle)
                    }
                    (mListView.adapter as ArrayAdapter<String>).notifyDataSetChanged()
                })

            })

        }
    }
}