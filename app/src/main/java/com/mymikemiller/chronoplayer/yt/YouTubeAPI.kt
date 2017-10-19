package com.mymikemiller.chronoplayer.yt

import android.accounts.Account
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.youtube.YouTube
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistListResponse
import com.mymikemiller.chronoplayer.Channel
import com.mymikemiller.chronoplayer.Detail
import com.mymikemiller.chronoplayer.DeveloperKey
import java.util.*
import com.google.api.services.youtube.model.PlaylistStatus
import com.google.api.services.youtube.model.PlaylistSnippet
import com.google.api.services.youtube.model.PlaylistItem
import com.google.api.services.youtube.model.ResourceId
import com.google.api.services.youtube.model.PlaylistItemSnippet
import com.mymikemiller.chronoplayer.PreferencesActivity


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

    fun cancelCommit() {
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

            getUserPlaylist(authenticatedYoutube, playlistTitle, { playlist ->
                // If we fail to find a playlist or a videoId, we callback with a blank string
                var videoId = ""
                if (playlist != null) {
                    var playlistItemsRequest = authenticatedYoutube.playlistItems().list("contentDetails,snippet")

                    playlistItemsRequest.playlistId = playlist.id
                    playlistItemsRequest.maxResults = 50
                    playlistItemsRequest.part = "snippet,contentDetails"
                    var pageToken: String? = null

                    while (true)
                    {
                        playlistItemsRequest.pageToken = pageToken;
                        var playlistItemsResponse = playlistItemsRequest.execute();
                        pageToken = playlistItemsResponse.nextPageToken;

                        if (pageToken == null) {
                            // We've reached the last page. Callback the last video id.
                            if (playlistItemsResponse.items.size > 0) {
                                videoId = playlistItemsResponse.items[playlistItemsResponse.items.size - 1].snippet.resourceId.videoId
                                break
                            }
                        }
                    }
                }

                // videoId will either be a valid videoId or "" (didn't find a playlist or didn't find a videoId)
                callback(videoId)
            })

            return
        }
    }

    fun addVideosToPlayList(playlistTitle: String, detailsToCommit: List<Detail>,
                            setPercentageCallback: (totalVideos: kotlin.Int, currentVideoNumber: kotlin.Int) -> Unit)
    {
        if (detailsToCommit.size == 0)
            return

        // Start out allowing commits. The user can cancel this commit
        // operation by calling YouTubeAPI.cancelCommit()
        mCommitCancelled = false

        getOrCreatePlaylist(playlistTitle, { playlist -> kotlin.run {
            for (index in 0..detailsToCommit.size - 1) {
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
                    setPercentageCallback(detailsToCommit.size, index + 1)
                } else {
                    break;
                }
            }
        }
        })
    }

    fun getDetailsToCommit(playlistTitle: String, details: List<Detail>, callback: (List<Detail>) -> Unit) {
        // Get the last videoId so we know where to start with the commit
        getLastVideoId(playlistTitle, { lastVideoId ->
            // Remove all videos before the last videoId
            val detailsToCommit = mutableListOf<Detail>()
            var found = false
            for(detail in details) {
                if (detail.videoId == lastVideoId) {
                    found = true
                }
                if (!found) {
                    detailsToCommit.add(detail)
                }
            }
            callback(detailsToCommit.toList().asReversed())
        })
        return
    }

    // Static functions that don't require authoriation
    companion object {
        // Scope for modifying the user's private data
        val YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube"

        var sYouTubeAPI: YouTubeAPI? = null

        /**
         * Define a global instance of a YouTube object, which will be used to make
         * YouTube Data API requests.
         */
        private val youtube: YouTube = YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                HttpRequestInitializer { }).setApplicationName("chronoplayer").build()


        fun isAuthenticated() : Boolean{
            return sYouTubeAPI != null
        }

        fun authenticate(context: Context, account: Account) {
            sYouTubeAPI = YouTubeAPI(context, account)
        }
        fun unAuthenticate() {
            sYouTubeAPI = null
        }

        fun getDetailsToCommit(playlistTitle: String, details: List<Detail>, callback: (List<Detail>) -> Unit) {
            if (isAuthenticated()) {
                sYouTubeAPI!!.getDetailsToCommit(playlistTitle, details, callback)
            } else {
                throw RuntimeException("Cannot getDetailsToCommmit. User is not authenticated.")
            }
        }

        fun cancelCommit() {
            if (isAuthenticated()) {
                sYouTubeAPI!!.cancelCommit()
            } else {
                throw RuntimeException("Cannot cancelCommit. User is not authenticated.")
            }
        }

        fun addVideosToPlayList(playlistTitle: String, detailsToCommit: List<Detail>,
                                setPercentageCallback: (totalVideos: kotlin.Int, currentVideoNumber: kotlin.Int) -> Unit) {
            if (isAuthenticated()) {
                sYouTubeAPI!!.addVideosToPlayList(playlistTitle, detailsToCommit, setPercentageCallback)
            } else {
                throw RuntimeException("Cannot addVideosToPlayList. User is not authenticated.")
            }
        }

        fun fetchAllDetails(channel: Channel,
                            stopAtDate: DateTime?,
                            incrementalDetailsFetched: (List<Detail>) -> Unit,
                            callback: (details: List<Detail>) -> Unit) {

            // Clear the details in preparation of fetching them all
            allDetails = mutableListOf<Detail>()

            FetchNextDetailTask(channel,
                    "",
                    stopAtDate,
                    accumulate,
                    incrementalDetailsFetched,
                    callback).execute()
        }

        var allDetails = mutableListOf<Detail>()
        var prevNextPageToken = ""

        val accumulate: (channel: Channel,
                         detailList: List<Detail>,
                         nextPageToken: String,
                         stopAtDate: DateTime?,
                         incrementalDetailsFetched: (List<Detail>) -> Unit,
                         callbackWhenDone: (detailList: List<Detail>) -> Unit
        ) -> Unit = { channel,
                      detailsList,
                      nextPageToken,
                      stopAtDate,
                      incrementalDetailsFetched,
                      callbackWhenDone ->
            run {
                prevNextPageToken = nextPageToken
                allDetails.addAll(detailsList)
                FetchNextDetailTask(channel,
                        nextPageToken,
                        stopAtDate,
                        accumulate,
                        incrementalDetailsFetched,
                        callbackWhenDone).execute()
            }
        }

        private class FetchNextDetailTask(val channel: Channel,
                                        var pageToken: String,
                                        val stopAtDate: DateTime?,
                                        val callback: (channel: Channel,
                                                 detailList: List<Detail>,
                                                 nextPageToken: String,
                                                 stopAtDate: DateTime?,
                                                 incrementalDetailsFetched: (List<Detail>) -> Unit,
                                                 callbackWhenDone: (detailList: List<Detail>) -> Unit) -> Unit,
                                        val incrementalDetailsFetched: (List<Detail>) -> Unit,
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

                        // Only stop if we specified a stopAtDate and the detail was uploaded before the stop at date
                        if (stopAtDate != null && d.dateUploaded.value < stopAtDate.value) {
                            // Don't break here because our results come in out of order, so we need
                            // to keep looping to make sure we get all the new stuff
                            done = true
                        }

                        results.add(d)
                    }

                    // Let the caller know how close we are to being done
                    val overallTotalResults = searchResponse.pageInfo.totalResults

                    // We have to add searchResultList.size instead of just allDetails.size because allDetails won't be added to until the callback is called below
                    incrementalDetailsFetched(results)

                    if (done || searchResponse.nextPageToken == null) {
                        // This is the last page of results.
                        // Call the final callback.
                         allDetails.addAll(results)
                         callbackWhenDone(allDetails)
                        return
                    }

                    callback(channel, results, searchResponse.nextPageToken, stopAtDate, incrementalDetailsFetched, callbackWhenDone)
                }
            }
        }

        fun getNumDetailsFromYouTube(channels: List<Channel>, callback: (Int) -> Unit) {
            GetNumDetailsFromYouTubeTask(channels, { numDetails ->
                run {
                    callback(numDetails)
                }
            }).execute()
        }

        private class GetNumDetailsFromYouTubeTask(val channels: List<Channel>,
                                        val callback: (numDetailsTotal: Int) -> Unit
        ) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg params: Unit?) {
                var totalVideos = 0
                for(channel in channels) {
                    val videosListByUploadPlaylistIdRequest = youtube.PlaylistItems().list("snippet")
                    videosListByUploadPlaylistIdRequest.playlistId = channel.uploadPlaylistId
                    videosListByUploadPlaylistIdRequest.key = (DeveloperKey.DEVELOPER_KEY)
                    videosListByUploadPlaylistIdRequest.maxResults = 50
                    videosListByUploadPlaylistIdRequest.pageToken = ""

                    val searchResponse = videosListByUploadPlaylistIdRequest.execute()
                    totalVideos += searchResponse.pageInfo.totalResults
                }
                callback(totalVideos)
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
            GetUserPlaylistsTask(authenticatedYoutube, { playlists ->
                run {
                    // Find the specified playlist
                    for (playlist in playlists) {
                        if (playlist.snippet.title == title) {
                            callback(playlist)
                            return@run
                        }
                    }
                    callback(null)
                }
            }).execute()
        }

        fun getUserPlaylistTitles(callback: (List<String>) -> Unit) {
            if (isAuthenticated()) {
                GetUserPlaylistsTask(sYouTubeAPI?.mYouTube!!, { playlists ->
                    run {
                        val playlistTitles = playlists.map { it -> it.snippet.title }
                        callback(playlistTitles)
                    }
                }).execute()
            }
        }

        /**
         * AsyncTask that uses the specified AuthYoutube playlist API to get all the user's playlists
         */
        private class GetUserPlaylistsTask(val authenticatedYoutube: YouTube, val callback: (List<Playlist>) -> Unit) : AsyncTask<String, Unit, Unit>() {

            override fun onPreExecute(): Unit {}

            override fun doInBackground(vararg params: String) {
                // Get all the user's playlists
                val playlistsList: PlaylistListResponse = authenticatedYoutube
                        .playlists()
                        .list("snippet,contentDetails")
                        .setMine(true)
                        .setMaxResults(25)
                        .execute()

                val playlists = playlistsList.getItems()

                callback(playlists)
            }

            override fun onPostExecute(result: Unit?) {}
        }

    }
}