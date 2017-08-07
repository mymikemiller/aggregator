package com.mymikemiller.gamegrumpsplayer

// todo: move this into a callback function
interface OnDetailsFetchedListener {
    fun onDetailsFetched(id: String, details: Details)
}

/**
*   Contains detailed information about the video, e.g. the thumbnail image, title and description
*/
data class Details(val fullVideoTitle: String,
                   val fullVideoDescription: String,
                   val thumbnail: String) {
    val team: String by lazy {
        val lastDashIndex = fullVideoTitle.lastIndexOf(" - ")
        fullVideoTitle.substring(lastDashIndex + 3, fullVideoTitle.length)
    }
    private val gameAndTitleAndPart: String by lazy {
        val lastDashIndex = fullVideoTitle.lastIndexOf(" - ")
        fullVideoTitle.substring(0, lastDashIndex)
    }
    private val gameAndTitle: String by lazy {
        val lastDashIndex = gameAndTitleAndPart.lastIndexOf(" - ")
        gameAndTitleAndPart.substring(0, lastDashIndex)
    }
    val game: String by lazy {
        val firstDashIndex = gameAndTitle.indexOf(" - ")
        gameAndTitle.substring(0, firstDashIndex)
    }
    val title: String by lazy {
        val lastDashIndex = gameAndTitle.lastIndexOf(" - ")
        gameAndTitle.substring(lastDashIndex + 3, gameAndTitle.length)
    }
    val part: String by lazy {
        val lastDashIndex = gameAndTitleAndPart.lastIndexOf(" - ")
        gameAndTitleAndPart.substring(lastDashIndex + 3, gameAndTitleAndPart.length)
    }
    val description: String by lazy {
        val firstNewline = fullVideoDescription.indexOf('\n')
        fullVideoDescription.substring(0, firstNewline)
    }

    companion object {
        fun fetchDetails(id: String, callback: OnDetailsFetchedListener) {
            YouTubeAPI.fetchDetails(id, callback)
        }
        private fun getTitle(fullVideoTitle: String) : String {
            return fullVideoTitle
        }
        private fun getGame(fullVideoTitle: String) : String {
            return fullVideoTitle
        }
    }
}
