package com.mymikemiller.chronoplayer.yt

import android.accounts.Account
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.youtube.YouTube
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistListResponse
import com.mymikemiller.chronoplayer.Channel
import com.mymikemiller.chronoplayer.Detail
import com.mymikemiller.chronoplayer.DeveloperKey
import com.mymikemiller.chronoplayer.util.CommitPlaylists
import java.io.IOException
import java.util.*
import com.google.api.services.youtube.model.PlaylistStatus
import com.google.api.services.youtube.model.PlaylistSnippet
import com.mymikemiller.chronoplayer.R.id.playlist
import com.google.api.services.youtube.model.PlaylistItem
import com.google.api.services.youtube.model.ResourceId
import com.google.api.services.youtube.model.PlaylistItemSnippet







val HTTP_TRANSPORT = NetHttpTransport()
val JSON_FACTORY: JsonFactory = JacksonFactory()

/**
 *  A helper class that communicates with YouTube to make requests and return data. To use
 *  authenticated requests, an instance of YouTubeAPI must be constructed. Otherwise, the static
 *  methods may be used.
 */
class YouTubeAPI(context: Context, account: Account) {
    private val mYouTube: YouTube
    private var mCommitCancelled = false

    init {
        val credential: GoogleAccountCredential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(YOUTUBE_SCOPE))
        credential.setSelectedAccount(account)

        // Global instance of the HTTP transport
        val HTTP_TRANSPORT: HttpTransport = AndroidHttp.newCompatibleTransport()

        // Global instance of the JSON factory
        val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance()

        mYouTube = YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName("ChronoPlayer")
                .build()
    }

    private fun getOrCreatePlaylist(title: String, callback: (Playlist) -> Unit) {
        getUserPlaylist(mYouTube, title, { playlist ->
            if (playlist != null) {
                // We found the playlist
                callback(playlist)
            } else {
                val createdPlaylist = createPlaylist(mYouTube, title)
                callback(createdPlaylist)
            }
        })
    }

    fun cancelCommmit() {
        mCommitCancelled = true
    }



    // We can't use Details here because a Detail knows what Channel it came from and we wouldn't
    // be able to compare details once we get it back. So we just use videoIds
    fun getLastVideoId(playlistTitle: String, callback: (String) -> Unit) {
        GetLastVideoIdTask(mYouTube, playlistTitle, { videoId ->
            run {
                callback(videoId)
            }
        }).execute()
    }

    private class GetLastVideoIdTask(val authenticatedYoutube: YouTube, val playlistTitle: String, val callback: (String) -> Unit) : AsyncTask<String, Unit, Unit>() {

        override fun onPreExecute(): Unit {}

        override fun doInBackground(vararg params: String) {

            //TODO: get user playlist

//            var playlistItemsRequest = authenticatedYoutube.playlistItems().list("contentDetails,snippet,id")
//            playlistItemsRequest.id = playlist.id
//            playlistItemsRequest.maxResults = 50
//            var pageToken: String? = null
//
//            while (true)
//            {
//                playlistItemsRequest.pageToken = pageToken;
//                var playlistItemsResponse = playlistItemsRequest.execute();
//                pageToken = playlistItemsResponse.nextPageToken;
//
//                if (pageToken == null) {
//                    // We've reached the last page. Return the last video id.
//                    callback(playlistItemsResponse.items[playlistItemsResponse.items.size - 1].snippet.resourceId.videoId);
//                    return
//                }
//            }
//            return
//
//            // we didn't find a playlist with the specified name. Log a message and simply return
//            Log.e("GetLastVideoId", "Couldn't find specified playlist")

            return
        }
    }



    fun addVideosToPlayList(playlistTitle: String, detailsToCommit: List<Detail>,
                            setPercentageCallback: (totalVideos: kotlin.Int, currentVideoNumber: kotlin.Int) -> Unit)
    {
        // Start out allowing commits. The user can cancel this commit
        // operation by calling YouTubeAPI.cancelCommit()
        mCommitCancelled = false

        getOrCreatePlaylist(playlistTitle, { playlist -> kotlin.run {
            for (index in 0..detailsToCommit.size) {
                if (!mCommitCancelled) {
                    val detail = detailsToCommit[index]
                    val videoId = detail.videoId

                    val playlistItem = PlaylistItem()
                    val snippet = PlaylistItemSnippet()
                    snippet.playlistId = playlist.id
                    val resourceId = ResourceId()
                    resourceId.set("kind", "youtube#video")
                    resourceId.set("videoId", videoId)

                    snippet.resourceId = resourceId
                    playlistItem.snippet = snippet

                    var request = mYouTube.playlistItems().insert("snippet", playlistItem)
                    request.execute()
                    setPercentageCallback(detailsToCommit.size, index)
                } else {
                    break;
                }
            }
        }
        })
    }

    // Static functions that don't require authoriation
    companion object {
        // Scope for modifying the user's private data
        val YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube"

        /**
         * Define a global instance of a YouTube object, which will be used to make
         * YouTube Data API requests.
         */
        private val youtube: YouTube = YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                HttpRequestInitializer { }).setApplicationName("chronoplayer").build()

        fun fetchAllDetails(channel: Channel,
                              stopAtDetail: Detail?,
                              setPercentageCallback: (totalVideos: kotlin.Int, currentVideoNumber: kotlin.Int) -> Unit,
                              callback: (details: List<Detail>) -> Unit) {

            // Clear the details in preparation of fetching them all
            allDetails = mutableListOf<Detail>()

            FetchNextDetailTask(channel,
                    "",
                    stopAtDetail,
                    accumulate,
                    setPercentageCallback,
                    callback).execute()
        }

        var allDetails = mutableListOf<Detail>()
        var prevNextPageToken = ""

        val accumulate: (channel: Channel,
                         detailList: List<Detail>,
                         nextPageToken: String,
                         stopAtDetail: Detail?,
                         setPercentageCallback: (totalVideos: kotlin.Int, currentVideoNumber: kotlin.Int) -> Unit,
                         callbackWhenDone: (detailList: List<Detail>) -> Unit
        ) -> Unit = { channel,
                      detailsList,
                      nextPageToken,
                      stopAtDetail,
                      setPercentageCallback,
                      callbackWhenDone ->
            run {
                prevNextPageToken = nextPageToken
                allDetails.addAll(detailsList)
                FetchNextDetailTask(channel,
                        nextPageToken,
                        stopAtDetail,
                        accumulate,
                        setPercentageCallback,
                        callbackWhenDone).execute()
            }
        }

        private class FetchNextDetailTask(val channel: Channel,
                                        var pageToken: String,
                                        val stopAtDetail: Detail?,
                                        val callback: (channel: Channel,
                                                 detailList: List<Detail>,
                                                 nextPageToken: String,
                                                 stopAtDetail: Detail?,
                                                 setPercentageCallback: (kotlin.Int, kotlin.Int) -> Unit,
                                                 callbackWhenDone: (detailList: List<Detail>) -> Unit) -> Unit,
                                        val setPercentageCallback: (totalVideos: kotlin.Int,
                                                      currentVideoNumber: kotlin.Int) -> Unit,
                                        val callbackWhenDone: (detailList: List<Detail>) -> Unit
                                              ) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg params: Unit?) {
                val videosListByUploadPlaylistIdRequest = youtube.PlaylistItems().list("snippet")
                videosListByUploadPlaylistIdRequest.playlistId = channel.uploadPlaylistId
                videosListByUploadPlaylistIdRequest.key = (DeveloperKey.DEVELOPER_KEY)
                videosListByUploadPlaylistIdRequest.maxResults = 50
                videosListByUploadPlaylistIdRequest.pageToken = pageToken

                val searchResponse = videosListByUploadPlaylistIdRequest.execute()
                var done = false
                val searchResultList = searchResponse.items
                if (searchResultList != null) {
                    val results: MutableList<Detail> = mutableListOf()
                    for (result in searchResultList) {
                        val d = createDetail(result, channel)

                        if (d == stopAtDetail) {
                            // Don't break here because our results come in out of order, so we need
                            // to keep looping to make sure we get all the new stuff
                            done = true
                        }

                        results.add(d)
                    }

                    // Let the caller know how close we are to being done
                    val overallTotalResults = searchResponse.pageInfo.totalResults
                    // We have to add searchResultList.size instead of just allDetails.size because allDetails won't be added to until the callback is called below
                    setPercentageCallback(Integer.valueOf(overallTotalResults), Integer.valueOf(allDetails.size + searchResultList.size))

                    if (done || searchResponse.nextPageToken == null) {
                        // This is the last page of results.
                        // Send the previous pageToken so we can cache the last pageToken to start on that page next time.
                        // Add them to allResults and call the final callback.
                        allDetails.addAll(results)
                        callbackWhenDone(allDetails)
                        return
                    }

                    callback(channel, results, searchResponse.nextPageToken, stopAtDetail, setPercentageCallback, callbackWhenDone)
                }
            }
        }

        fun createDetail(item: PlaylistItem, channel: Channel): Detail {

            val thumbnail = if (item.snippet.thumbnails.standard != null) item.snippet.thumbnails.standard.url else item.snippet.thumbnails.high.url

            val d = Detail(channel,
                    item.snippet.resourceId.videoId,
                    item.snippet.title,
                    item.snippet.description,
                    thumbnail,
                    item.snippet.publishedAt)

            return d
        }

        fun fetchChannels(query: String, callback: (channels: List<Channel>) -> Unit) {
            FetchChannelsTask(query, callback).execute()
        }

        private class FetchChannelsTask(val query: String, val callback: (channels: List<Channel>) -> Unit) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg params: Unit?) {
                val parameters = hashMapOf<String, String>()
                parameters.put("part", "snippet")
                parameters.put("maxResults", "25")
                parameters.put("q", query)
                parameters.put("type", "channel")

                val searchListByKeywordRequest = youtube.search().list(parameters.get("part").toString())
                searchListByKeywordRequest.key = DeveloperKey.DEVELOPER_KEY

                if (parameters.containsKey("maxResults")) {
                    searchListByKeywordRequest.setMaxResults((parameters.get("maxResults").toString()).toLong())
                }
                if (parameters.containsKey("q") && parameters.get("q") != "") {
                    searchListByKeywordRequest.setQ(parameters.get("q").toString())
                }

                if (parameters.containsKey("type") && parameters.get("type") != "") {
                    searchListByKeywordRequest.setType(parameters.get("type").toString())
                }
                val response = searchListByKeywordRequest.execute()

                val channels = mutableListOf<Channel>()
                for (item in response.items) {
                    fetchUploadPlaylistId(item.snippet.channelId, { uploadPlaylistId ->
                        run {

                            val channel = Channel(
                                    item.snippet.channelId,
                                    item.snippet.channelTitle,
                                    uploadPlaylistId,
                                    item.snippet.thumbnails.default.url)
                            channels.add(channel)

                            callback(channels)
                        }
                    })
                }
            }
        }


        fun fetchUploadPlaylistId(channelId: String, callback: (channels: String) -> Unit) {
            FetchUploadPlaylistIdTask(channelId, callback).execute()
        }

        private class FetchUploadPlaylistIdTask(val channelId: String, val callback: (uploadId: String) -> Unit) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg params: Unit?) {
                val parameters = hashMapOf<String, String>()
                parameters.put("part", "contentDetails")
                parameters.put("id", channelId)

                val uploadIdRequest = youtube.channels().list(parameters.get("part").toString())
                uploadIdRequest.key = DeveloperKey.DEVELOPER_KEY

                if (parameters.containsKey("part")) {
                    uploadIdRequest.setPart((parameters.get("part").toString()))
                }
                if (parameters.containsKey("id") && parameters.get("id") != "") {
                    uploadIdRequest.setId(parameters.get("id").toString())
                }

                val response = uploadIdRequest.execute()
                val uploadPlaylistId = response.items[0].contentDetails.relatedPlaylists.uploads

                callback(uploadPlaylistId)
            }
        }

        fun createPlaylist(authenticatedYoutube: YouTube, title: String): Playlist{
            val playlist = Playlist()
            val snippet = PlaylistSnippet()
            val status = PlaylistStatus()

            playlist.snippet = snippet
            playlist.snippet.title = title
            playlist.status = status

            val playlistsInsertRequest = authenticatedYoutube.playlists().insert("snippet,status", playlist)

            val response = playlistsInsertRequest.execute()
            return response
        }

        private fun getUserPlaylist(authenticatedYoutube: YouTube, title: String, callback: (Playlist?) -> Unit) {
            GetUserPlaylistTask(authenticatedYoutube, title, { playlist ->
                run {
                    callback(playlist)
                }
            }).execute()
        }

        /**
         * AsyncTask that uses the specified AuthYoutube playlist API.
         */
        private class GetUserPlaylistTask(val authenticatedYoutube: YouTube, val title: String, val callback: (Playlist?) -> Unit) : AsyncTask<String, Unit, Unit>() {

            override fun onPreExecute(): Unit {}

            override fun doInBackground(vararg params: String) {
                // Get all the user's playlists
                val playlistsList: PlaylistListResponse = authenticatedYoutube
                        .playlists()
                        .list("snippet")
                        .setMine(true)
                        .execute()

                val playlists = playlistsList.getItems()

                // Find the specified playlist
                for (playlist in playlists) {
                    if (playlist.snippet.title == title) {
                        callback(playlist)
                        return
                    }
                }

                // We didn't find the playlist. Call the callback specifying null.
                callback(null)
            }

            override fun onPostExecute(result: Unit?) {}
        }
    }
}