package com.mymikemiller.gamegrumpsplayer


import android.os.AsyncTask
import com.google.api.services.youtube.YouTube
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory

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

        fun fetchDetails(id:String, callback: (Details) -> Unit) {

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
                            video.getSnippet().getTitle(),
                            video.getSnippet().description,
                            video.getSnippet().thumbnails.maxres.url)//video.getSnippet().thumbnails[0])

                    callback(details)
                }
            }

            override fun onPostExecute(result: Unit?) {
            }
        }
    }
}
