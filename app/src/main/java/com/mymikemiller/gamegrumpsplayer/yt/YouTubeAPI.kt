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

        fun fetchDetails(id: String, callback: (Details) -> Unit) {

            val parameters = HashMap<String, String>()
            parameters.put("part", "snippet,contentDetails,statistics")
            parameters.put("id", id)

            val videosListByIdRequest = youtube.videos().list(parameters.get("part").toString())
            if (parameters.containsKey("id") && parameters.get("id") !== "") {
                videosListByIdRequest.id = parameters.get("id").toString()
            }
            videosListByIdRequest.setKey(DeveloperKey.DEVELOPER_KEY)
            videosListByIdRequest.setMaxResults(1)

            val searchTask = FindVideoByIdTask(videosListByIdRequest, callback)
            searchTask.execute()
        }

        class FindVideoByIdTask(val search: YouTube.Videos.List, val callback: (Details) -> Unit) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg params: Unit?) {
                // Call the API and print results.
                val searchResponse = search.execute()
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

            override fun onPostExecute(result: Unit?) {
            }
        }

        fun FindChannelIdByChannelName(channelName: String, callback: (channelId: String) -> Unit) {
            val channelsListByUsernameRequest = youtube.channels().list("snippet,contentDetails,statistics")
            channelsListByUsernameRequest.forUsername = channelName
            channelsListByUsernameRequest.setKey(DeveloperKey.DEVELOPER_KEY)
            channelsListByUsernameRequest.setMaxResults(1)

            val searchTask = FindChannelIdByChannelNameTask(channelsListByUsernameRequest, callback)
            searchTask.execute()
        }

        class FindChannelIdByChannelNameTask(val search: YouTube.Channels.List, val callback: (String) -> Unit) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg params: Unit?) {
                // Call the API and print results.
                val searchResponse = search.execute()
                val searchResultList = searchResponse.getItems()
                if (searchResultList != null && searchResultList.size == 1) {
                    val channelInfo = searchResultList[0]

                    // We have to replace the second character with a "U" for some reason
                    val channelId = channelInfo.id
                    val first = channelId.substring(0, 1)
                    val last = channelId.substring(2, channelId.length)
                    val actualChannelId = first + "U" + last

                    callback(actualChannelId)

                    val listVideos: (List<String>) -> Unit = { videoIds ->
                        run {
                            for (videoId in videoIds) {
                                println(videoId)
                            }
                        }
                    }

                    GetAllVideosByChannelId(actualChannelId, listVideos)
                }
            }

            override fun onPostExecute(result: Unit?) {
            }
        }

        fun GetAllVideosByChannelId(channelId: String, callback: (videoIds: List<String>) -> Unit) {
            val videosListByChannelIdRequest = youtube.PlaylistItems().list("snippet")
            videosListByChannelIdRequest.playlistId = channelId
            videosListByChannelIdRequest.key = (DeveloperKey.DEVELOPER_KEY)
            videosListByChannelIdRequest.setMaxResults(50)

            val searchTask = FindAllVideosByChannelIdTask(videosListByChannelIdRequest, callback)
            searchTask.execute()
        }

        class FindAllVideosByChannelIdTask(val search: YouTube.PlaylistItems.List, val callback: (List<String>) -> Unit) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg params: Unit?) {
                val searchResponse = search.execute()
                val searchResultList = searchResponse.getItems()
                if (searchResultList != null) {
                    val results: MutableList<String> = mutableListOf()
                    for (result in searchResultList) {
                        results.add(result.snippet.resourceId.videoId)
                        println(result)
                    }
                    callback(results)
                }
            }

            override fun onPostExecute(result: Unit?) {
            }
        }
    }
}