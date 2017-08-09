package com.mymikemiller.gamegrumpsplayer

import com.mymikemiller.gamegrumpsplayer.yt.YouTubeAPI

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
        // There are 3 possible cases for gameTitleAndPart:
        // 1: First video case: Game - Title
        // 2: Old video case: Game - Title - Part
        // 3: New video case: Game: Title - Part
        val gameAndTitle: String

        if (gameAndTitleAndPart.split(" - ").size == 2) {
            // Case 1 or 3 (a single dash separator)
            val firstDashSeparatorIndex = gameAndTitleAndPart.indexOf(" - ")
            val firstHalf = gameAndTitleAndPart.substring(0, firstDashSeparatorIndex)

            if (firstHalf.split(": ").size == 2) {
                // Case 3 (colon separated firstHalf). gameAndTitle should be the whole firstHalf
                gameAndTitle = firstHalf
            } else {
                // Case 1 (no colon in first half). gameAndTitle should be the original
                // gameAndTitleAndPart (no part specified)
                gameAndTitle = gameAndTitleAndPart
            }
        } else {
            // Case 2 (two dashes)
            val lastSeparatorDashIndex = gameAndTitleAndPart.lastIndexOf(" - ")
            gameAndTitle = gameAndTitleAndPart.substring(0, lastSeparatorDashIndex)
        }

        gameAndTitle
    }
    val game: String by lazy {
        val firstDashIndex = gameAndTitle.indexOf(" - ")

        val game: String
        if (firstDashIndex == -1) {
            val lastColonIndex = gameAndTitle.lastIndexOf(": ")
            game = gameAndTitle.substring(0, lastColonIndex)
        } else {
            game = gameAndTitle.substring(0, firstDashIndex)
        }
        game
    }
    val title: String by lazy {
        val title: String
        val firstDashIndex = gameAndTitle.indexOf(" - ")
        if (firstDashIndex == -1) {
            val firstColonIndex = gameAndTitle.indexOf(": ")
            title = gameAndTitle.substring(firstColonIndex + 2, gameAndTitle.length)
        } else {
            title = gameAndTitle.substring(firstDashIndex + 3, gameAndTitle.length)
        }
        title
    }
    val part: String by lazy {
        val part: String
        val firstSeparatorDashIndex = gameAndTitleAndPart.indexOf(" - ")
        val lastSeparatorDashIndex = gameAndTitleAndPart.lastIndexOf(" - ")

        if (firstSeparatorDashIndex == lastSeparatorDashIndex) {
            // If there's only one dash, it's either that the "part" wasn't included, e.g.
            // Kirby Super Star - Spring Breeze ADVENTURES! - GameGrumps
            // or that the game and title are separated by a colon
            val colonIndex = gameAndTitleAndPart.indexOf(": ")
            if (colonIndex != -1) {
                // We found a colon. Parse out the part, which is everything after the last (only)
                // dash
                part = gameAndTitleAndPart.substring(lastSeparatorDashIndex + 3,
                        gameAndTitleAndPart.length)
            } else {
                // No colon, so we must just not have a part specified
                part = ""
            }
        } else {
            // There are two dashes, so the part is after the last dash
            part = gameAndTitleAndPart.substring(lastSeparatorDashIndex + 3,
                    gameAndTitleAndPart.length)
        }
        part
    }
    val description: String by lazy {
        val firstNewline = fullVideoDescription.indexOf('\n')
        fullVideoDescription.substring(0, firstNewline)
    }

    companion object {
        fun fetchDetails(id: String, callback: (Details) -> Unit) {
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
