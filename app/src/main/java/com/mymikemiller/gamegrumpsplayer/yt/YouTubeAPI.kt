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

        fun fetchDetailsForVideo(id: String, callback: (Details) -> Unit) {
            FetchDetailsForVideoTask(id, callback).execute()
        }

        fun fetchChannelIdFromChannelName(channelName: String, callback: (channelId: String) -> Unit) {
            FetchChannelIdFromChannelNameTask(channelName, callback).execute()
        }

        fun fetchAllDetailsByChannelId(channelId: String, callback: (videoIds: List<Details>) -> Unit) {
            FetchAllDetailsByChannelIdTask(channelId, callback).execute()
        }



        private class FetchDetailsForVideoTask(val id: String, val callback: (Details) -> Unit) : AsyncTask<Unit, Unit, Unit>() {
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

        private class FetchChannelIdFromChannelNameTask(val channelName: String, val callback: (String) -> Unit) : AsyncTask<Unit, Unit, Unit>() {
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

                    callback(actualChannelId)
                }
            }
        }

        class FetchAllDetailsByChannelIdTask(val channelId: String, val callback: (List<Details>) -> Unit) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg params: Unit?) {
                val videosListByChannelIdRequest = youtube.PlaylistItems().list("snippet")
                videosListByChannelIdRequest.playlistId = channelId
                videosListByChannelIdRequest.key = (DeveloperKey.DEVELOPER_KEY)
                videosListByChannelIdRequest.setMaxResults(50)

                val searchResponse = videosListByChannelIdRequest.execute()
                val searchResultList = searchResponse.getItems()
                if (searchResultList != null) {
                    val results: MutableList<Details> = mutableListOf()
                    for (result in searchResultList) {
                        val d = Details(result.snippet.resourceId.videoId,
                                result.snippet.title,
                                result.snippet.description,
                                result.snippet.thumbnails.standard.url)

                        results.add(d)
                    }
                    callback(results)
                }
            }
        }
    }
}