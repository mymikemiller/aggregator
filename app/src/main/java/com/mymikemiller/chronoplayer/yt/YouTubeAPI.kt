package com.mymikemiller.chronoplayer.yt

import android.os.AsyncTask
import com.google.api.services.youtube.YouTube
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.mymikemiller.chronoplayer.Channel
import com.mymikemiller.chronoplayer.Detail
import com.mymikemiller.chronoplayer.DeveloperKey

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
                HttpRequestInitializer { }).setApplicationName("chrono-player").build()

        fun fetchAllDetailsByUploadPlaylistId(uploadPlaylistId: String,
                                              stopAtDetail: Detail?,
                                              setPercentageCallback: (totalVideos: kotlin.Int, currentVideoNumber: kotlin.Int) -> Unit,
                                              callback: (details: List<Detail>) -> Unit) {
            FetchNextDetailByUploadPlaylistIdTask(uploadPlaylistId,
                    "",
                    stopAtDetail,
                    accumulate,
                    setPercentageCallback,
                    callback).execute()
        }

        val allDetails = mutableListOf<Detail>()
        var prevNextPageToken = ""

        val accumulate: (uploadPlaylistId: String,
                         detailList: List<Detail>,
                         nextPageToken: String,
                         stopAtDetail: Detail?,
                         setPercentageCallback: (totalVideos: kotlin.Int, currentVideoNumber: kotlin.Int) -> Unit,
                         callbackWhenDone: (detailList: List<Detail>) -> Unit
        ) -> Unit = { uploadPlaylistId,
                      detailsList,
                      nextPageToken,
                      stopAtDetail,
                      setPercentageCallback,
                      callbackWhenDone ->
            run {
                prevNextPageToken = nextPageToken
                allDetails.addAll(detailsList)
                FetchNextDetailByUploadPlaylistIdTask(uploadPlaylistId,
                        nextPageToken,
                        stopAtDetail,
                        accumulate,
                        setPercentageCallback,
                        callbackWhenDone).execute()
            }
        }

        private class FetchNextDetailByUploadPlaylistIdTask(val uploadPlaylistId: String,
                                                            var pageToken: String,
                                                            val stopAtDetail: Detail?,
                                                            val callback: (uploadPlaylistId: String,
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
                videosListByUploadPlaylistIdRequest.playlistId = uploadPlaylistId
                videosListByUploadPlaylistIdRequest.key = (DeveloperKey.DEVELOPER_KEY)
                videosListByUploadPlaylistIdRequest.maxResults = 50
                videosListByUploadPlaylistIdRequest.pageToken = pageToken

                val searchResponse = videosListByUploadPlaylistIdRequest.execute()
                var done = false
                val searchResultList = searchResponse.items
                if (searchResultList != null) {
                    val results: MutableList<Detail> = mutableListOf()
                    for (result in searchResultList) {
                        val thumbnail = if (result.snippet.thumbnails.standard != null) result.snippet.thumbnails.standard.url else result.snippet.thumbnails.high.url

                        val d = Detail(result.snippet.resourceId.videoId,
                                result.snippet.title,
                                result.snippet.description,
                                thumbnail,
                                result.snippet.publishedAt)

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

                    callback(uploadPlaylistId, results, searchResponse.nextPageToken, stopAtDetail, setPercentageCallback, callbackWhenDone)
                }
            }
        }

        fun fetchChannels(query: String, callback: (channels: List<Channel>) -> Unit) {
            FetchChannelsTask(query, callback).execute()
        }

        private class FetchChannelsTask(val query: String, val callback: (channels: List<Channel>) -> Unit) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg params: Unit?) {
                val parameters = hashMapOf<String, String>()
                parameters.put("part", "snippet");
                parameters.put("maxResults", "25");
                parameters.put("q", query);
                parameters.put("type", "channel");

                val searchListByKeywordRequest = youtube.search().list(parameters.get("part").toString());
                searchListByKeywordRequest.key = DeveloperKey.DEVELOPER_KEY

                if (parameters.containsKey("maxResults")) {
                    searchListByKeywordRequest.setMaxResults((parameters.get("maxResults").toString()).toLong());
                }
                if (parameters.containsKey("q") && parameters.get("q") != "") {
                    searchListByKeywordRequest.setQ(parameters.get("q").toString());
                }

                if (parameters.containsKey("type") && parameters.get("type") != "") {
                    searchListByKeywordRequest.setType(parameters.get("type").toString());
                }
                val response = searchListByKeywordRequest.execute();
                System.out.println(response);

                val channels = mutableListOf<Channel>()
                for (item in response.items) {
                    fetchUploadPlaylistId(item.snippet.channelId, { uploadPlaylistId ->
                        run {

                            val channel = Channel(item.snippet.channelTitle,
                                    item.snippet.channelId,
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
                parameters.put("part", "contentDetails");
                parameters.put("id", channelId);

                val uploadIdRequest = youtube.channels().list(parameters.get("part").toString());
                uploadIdRequest.key = DeveloperKey.DEVELOPER_KEY

                if (parameters.containsKey("part")) {
                    uploadIdRequest.setPart((parameters.get("part").toString()));
                }
                if (parameters.containsKey("id") && parameters.get("id") != "") {
                    uploadIdRequest.setId(parameters.get("id").toString());
                }

                val response = uploadIdRequest.execute();
                val uploadPlaylistId = response.items[0].contentDetails.relatedPlaylists.uploads

                callback(uploadPlaylistId)
            }
        }
    }
}