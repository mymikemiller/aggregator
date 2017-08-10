package com.mymikemiller.gamegrumpsplayer.yt

import android.os.AsyncTask
import com.google.api.services.youtube.YouTube
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.mymikemiller.gamegrumpsplayer.Details
import com.mymikemiller.gamegrumpsplayer.DeveloperKey

val HTTP_TRANSPORT = NetHttpTransport()
val JSON_FACTORY: JsonFactory = JacksonFactory()

/**
 *  A helper class that communicates with YouTube to make requests and return data
 */
class YouTubeAPI {
    companion object {
        /**
         * Define a global instance of a YouTube object, which will be used to make
         * YouTube Data API requests.
         */
        private val youtube: YouTube = YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                HttpRequestInitializer { }).setApplicationName("game-grumps-player").build()

        private var mChannelName = ""
        private var mChannelId = ""

        fun fetchDetailsForVideo(id: String, callback: (Details) -> Unit) {
            FetchDetailsForVideoTask(id, callback).execute()
        }

        fun fetchChannelIdFromChannelName(channelName: String, callback: (channelId: String) -> Unit) {
            if (mChannelName == "" || mChannelId == "") {
                FetchChannelIdFromChannelNameTask(channelName, callback).execute()
            } else {
                callback(mChannelId)
            }
        }

        fun fetchAllDetailsByChannelId(channelId: String, callback: (details: List<Details>) -> Unit) {
            FetchNextDetailsByChannelIdTask(channelId, "", accumulate, callback).execute()
        }



        private class FetchDetailsForVideoTask(val id: String, val callback: (details: Details) -> Unit) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg params: Unit?) {
                val videosListByIdRequest = youtube.videos().list("snippet,contentDetails,statistics")
                videosListByIdRequest.id = id
                videosListByIdRequest.key = DeveloperKey.DEVELOPER_KEY
                videosListByIdRequest.maxResults = 1

                // Call the API and print results.
                val searchResponse = videosListByIdRequest.execute()
                val searchResultList = searchResponse.getItems()
                if (searchResultList != null && searchResultList.size == 1) {
                    val video = searchResultList[0]

                    val details = Details(
                            video.id,
                            video.snippet.title,
                            video.snippet.description,
                            video.snippet.thumbnails.maxres.url)

                    callback(details)
                }
            }
        }

        private class FetchChannelIdFromChannelNameTask(val channelName: String, val callback: (channelId: String) -> Unit) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg params: Unit?) {
                val channelsListByUsernameRequest = youtube.channels().list("snippet,contentDetails,statistics")
                channelsListByUsernameRequest.forUsername = channelName
                channelsListByUsernameRequest.setKey(DeveloperKey.DEVELOPER_KEY)
                channelsListByUsernameRequest.setMaxResults(1)

                // Call the API and print results.
                val searchResponse = channelsListByUsernameRequest.execute()
                val searchResultList = searchResponse.getItems()
                if (searchResultList != null && searchResultList.size == 1) {
                    val channelInfo = searchResultList[0]

                    // We have to replace the second character with a "U" for some reason
                    val channelId = channelInfo.id
                    val first = channelId.substring(0, 1)
                    val last = channelId.substring(2, channelId.length)
                    val actualChannelId = first + "U" + last

                    // Cache the channelId so we don't have to find it again
                    mChannelName = channelName
                    mChannelId = actualChannelId
                    callback(actualChannelId)
                }
            }
        }

        val allDetails = mutableListOf<Details>()
        var prevNextPageToken = ""

        val accumulate: (channelId: String,
                         detailsList: List<Details>,
                         nextPageToken: String,
                         callbackWhenDone: (detailsList: List<Details>) -> Unit
        ) -> Unit = { channelId, detailsList, nextPageToken, callbackWhenDone ->
            run {
                if (nextPageToken == "CNAoEAA") {
                    println("Hello")
                }
                prevNextPageToken = nextPageToken
                allDetails.addAll(detailsList)
                FetchNextDetailsByChannelIdTask(channelId, nextPageToken, accumulate, callbackWhenDone ).execute()
            }
        }

        class FetchNextDetailsByChannelIdTask(val channelId: String,
                                              var pageToken: String,
                                              val callback: (channelId: String, detailsList: List<Details>, nextPageToken: String, callbackWhenDone: (detailsList: List<Details>) -> Unit) -> Unit,
                                              val callbackWhenDone: (detailsList: List<Details>) -> Unit
                                              ) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg params: Unit?) {
                val videosListByChannelIdRequest = youtube.PlaylistItems().list("snippet")
                videosListByChannelIdRequest.playlistId = channelId
                videosListByChannelIdRequest.key = (DeveloperKey.DEVELOPER_KEY)
                videosListByChannelIdRequest.setMaxResults(50)
                videosListByChannelIdRequest.pageToken = pageToken

                val searchResponse = videosListByChannelIdRequest.execute()
                val searchResultList = searchResponse.getItems()
                if (searchResultList != null) {
                    val results: MutableList<Details> = mutableListOf()
                    for (result in searchResultList) {
                        val thumbnail = if (result.snippet.thumbnails.standard != null) result.snippet.thumbnails.standard.url else result.snippet.thumbnails.high.url
                        val d = Details(result.snippet.resourceId.videoId,
                                result.snippet.title,
                                result.snippet.description,
                                thumbnail)

                        results.add(d)
                    }
                    if (searchResponse.nextPageToken == null) {
                        println("hi") // add a callbackWhenDone here?
                        callbackWhenDone(allDetails)
                        return
                    }
                    callback(channelId, results, searchResponse.nextPageToken, callbackWhenDone)
                }
            }
        }
    }
}