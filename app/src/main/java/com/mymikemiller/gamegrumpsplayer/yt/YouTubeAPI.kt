package com.mymikemiller.gamegrumpsplayer.yt

import android.os.AsyncTask
import com.google.api.services.youtube.YouTube
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.mymikemiller.gamegrumpsplayer.Detail
import com.mymikemiller.gamegrumpsplayer.DeveloperKey
import java.util.*

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

        // Hardcode Game Grumps values so we don't have to fetch them
        private var mChannelId = "UU9CuvdOVfMPvKCiwdGKL3cQ"

        fun fetchDetailForVideo(id: String, callback: (Detail) -> Unit) {
            FetchDetailForVideoTask(id, callback).execute()
        }

        fun fetchChannelIdFromChannelName(channelName: String, callback: (channelId: String) -> Unit) {
            if (mChannelId == "") {
                FetchChannelIdFromChannelNameTask(channelName, callback).execute()
            } else {
                callback(mChannelId)
            }
        }

        fun fetchAllDetailsByChannelId(channelId: String,
                                       startPageToken: String,
                                       setPercentageCallback: (totalVideos: kotlin.Int, currentVideoNumber: kotlin.Int) -> Unit,
                                       callback: (details: List<Detail>, finalPageToken: String) -> Unit) {
            FetchNextDetailByChannelIdTask(channelId,
                    startPageToken, // first :"", last: CIIpEAA, second to last: CNAoEAA
                    accumulate,
                    setPercentageCallback,
                    callback).execute()
        }



        private class FetchDetailForVideoTask(val id: String, val callback: (detail: Detail) -> Unit) : AsyncTask<Unit, Unit, Unit>() {
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

                    val detail = Detail(
                            video.id,
                            video.snippet.title,
                            video.snippet.description,
                            video.snippet.thumbnails.maxres.url,
                            video.snippet.publishedAt)

                    callback(detail)
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
                    mChannelId = actualChannelId
                    callback(actualChannelId)
                }
            }
        }

        val allDetails = mutableListOf<Detail>()
        var prevNextPageToken = ""

        val accumulate: (channelId: String,
                         detailList: List<Detail>,
                         nextPageToken: String,
                         setPercentageCallback: (totalVideos: kotlin.Int, currentVideoNumber: kotlin.Int) -> Unit,
                         callbackWhenDone: (detailList: List<Detail>, finalPageToken: String) -> Unit
        ) -> Unit = { channelId,
                      detailsList,
                      nextPageToken,
                      setPercentageCallback,
                      callbackWhenDone ->
            run {
                prevNextPageToken = nextPageToken
                allDetails.addAll(detailsList)
                FetchNextDetailByChannelIdTask(channelId,
                        nextPageToken,
                        accumulate,
                        setPercentageCallback,
                        callbackWhenDone).execute()
            }
        }

        private class FetchNextDetailByChannelIdTask(val channelId: String,
                                                      var pageToken: String,
                                                      val callback: (channelId: String,
                                                                     detailList: List<Detail>,
                                                                     nextPageToken: String,
                                                                     setPercentageCallback: (kotlin.Int, kotlin.Int) -> Unit,
                                                                     callbackWhenDone: (detailList: List<Detail>, finalPageToken: String) -> Unit) -> Unit,
                                                      val setPercentageCallback: (totalVideos: kotlin.Int,
                                                                          currentVideoNumber: kotlin.Int) -> Unit,
                                                      val callbackWhenDone: (detailList: List<Detail>, finalPageToken: String) -> Unit
                                              ) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg params: Unit?) {
                val videosListByChannelIdRequest = youtube.PlaylistItems().list("snippet")
                videosListByChannelIdRequest.playlistId = channelId
                videosListByChannelIdRequest.key = (DeveloperKey.DEVELOPER_KEY)
                videosListByChannelIdRequest.maxResults = 50
                videosListByChannelIdRequest.pageToken = pageToken

                val searchResponse = videosListByChannelIdRequest.execute()

                val searchResultList = searchResponse.getItems()
                if (searchResultList != null) {
                    val results: MutableList<Detail> = mutableListOf()
                    for (result in searchResultList) {
                        val thumbnail = if (result.snippet.thumbnails.standard != null) result.snippet.thumbnails.standard.url else result.snippet.thumbnails.high.url

                        val d = Detail(result.snippet.resourceId.videoId,
                                result.snippet.title,
                                result.snippet.description,
                                thumbnail,
                                result.snippet.publishedAt)
                                // TODO: Fix this, maybe. Use publishedAt

                        results.add(d)
                    }

                    // Let the caller know how close we are to being done
                    val overallTotalResults = searchResponse.pageInfo.totalResults
                    // We have to add searchResultList.size instead of just allDetails.size because allDetails won't be added to until the callback is called below
                    setPercentageCallback(Integer.valueOf(overallTotalResults), Integer.valueOf(allDetails.size + searchResultList.size))

                    // Find a Detail with the right name so we stop at the right page for testing purposes
//                    var testReturn = false
//                    for(d in allDetails) {
//                        if (d.title == "Ice Cream and Bagels") {
//                            testReturn = true;
//                        }
//                    }

                    if (searchResponse.nextPageToken == null || testReturn) {
                        // This is the last page of results.
                        // Send the previous pageToken so we can cache the last pageToken to start on that page next time.
                        // Add them to allResults and call the final callback.
                        allDetails.addAll(results)
                        callbackWhenDone(allDetails, searchResponse.prevPageToken)
                        return
                    }
                    callback(channelId, results, searchResponse.nextPageToken, setPercentageCallback, callbackWhenDone)
                }
            }
        }
    }
}