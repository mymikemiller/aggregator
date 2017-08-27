package com.mymikemiller.gamegrumpsplayer

import com.google.api.client.util.DateTime
import java.util.Date
import com.mymikemiller.gamegrumpsplayer.yt.YouTubeAPI
import java.text.SimpleDateFormat

/**
*   Contains detailed information about the video, e.g. the thumbnail image, title and description
*/
data class Detail(val videoId: String,
                  val fullVideoTitle: String,
                  val fullVideoDescription: String,
                  val thumbnail: String,
                  val dateUploaded: DateTime) : Comparable<Detail> {

    override fun equals(other: Any?): Boolean {
        if (other != null && other is Detail) {
            val otherDetail = other
            return videoId == otherDetail.videoId
        }
        return false
    }

    override fun hashCode(): Int {
        return videoId.hashCode()
    }

    val team: String by lazy {
        val team: String
        if (fullVideoTitle == "Zelda: A Link to the Past - Bad With Money - Part 3") {
            team = "Game Grumps"
        } else {
            val lastDashIndex = fullVideoTitle.lastIndexOf(" - ")
            if (lastDashIndex == -1) {
                // No dashes found. Return the whole title.
                team = fullVideoTitle
            } else {
                team = fullVideoTitle.substring(lastDashIndex + 3, fullVideoTitle.length)
            }
        }
        team
    }

    val gameAndTitleAndPart: String by lazy {
        // If no part found. Return the full video title
        var gameAndTitleAndPart = fullVideoTitle
        val lastDashIndex = fullVideoTitle.lastIndexOf(" - ")
        if (lastDashIndex != -1) {
            gameAndTitleAndPart = fullVideoTitle.substring(0, lastDashIndex)
        }
        gameAndTitleAndPart
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
            // Case 2 (two or no dashes)
            val lastSeparatorDashIndex = gameAndTitleAndPart.lastIndexOf(" - ")
            if (lastSeparatorDashIndex == -1) {
                // No separators found. Just return the whole thing as the gameAndTitle.
                gameAndTitle = gameAndTitleAndPart
            } else {
                gameAndTitle = gameAndTitleAndPart.substring(0, lastSeparatorDashIndex)
            }
        }

        gameAndTitle
    }
    val game: String by lazy {
        val game: String
        // This one's weird because the colon is in the game name so hard code it
         if (gameAndTitle == "Zelda: A Link to the Past") {
             game = "Zelda: A Link to the Past"
         } else {
            val firstDashIndex = gameAndTitle.indexOf(" - ")

            if (firstDashIndex == -1) {
                val lastColonIndex = gameAndTitle.lastIndexOf(": ")
                if (lastColonIndex == -1) {
                    // No dash and no colon found. We'll just have the game be the whole string.
                    game = gameAndTitle
                } else {
                    game = gameAndTitle.substring(0, lastColonIndex)
                }
            } else {
                game = gameAndTitle.substring(0, firstDashIndex)
            }
         }
        game
    }
    val title: String by lazy {
        val title: String
        // This one's weird because the colon is in the game title. Hard code it.
        if (gameAndTitleAndPart == "Zelda: A Link to the Past - Bad With Money") {
            title = "Bad With Money"
        } else {
            val firstDashIndex = gameAndTitle.indexOf(" - ")
            if (firstDashIndex == -1) {
                val firstColonIndex = gameAndTitle.indexOf(": ")
                if (firstColonIndex == -1) {
                    // No colon and no dash found. Just return the full game and title.
                    title = gameAndTitle
                } else {
                    title = gameAndTitle.substring(firstColonIndex + 2, gameAndTitle.length)
                }
            } else {
                title = gameAndTitle.substring(firstDashIndex + 3, gameAndTitle.length)
            }
        }
        title
    }
    val part: String by lazy {
        val part: String
        // This one's weird because of the colon in the game name. Hard code it.
        if (fullVideoTitle == "Zelda: A Link to the Past - Bad With Money - Part 3") {
            part = "Part 3"
        } else if (fullVideoTitle == "Joe & Mac: Finale - Game Grumps") {
            part = "Part 4"
        } else {
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
        }
        part
    }
    val description: String by lazy {
        var firstNewline = fullVideoDescription.indexOf('\n')
        if (firstNewline == -1) {
            println("Failed finding description in $fullVideoDescription")
            firstNewline = fullVideoDescription.length - 1
        }
        fullVideoDescription.substring(0, firstNewline)
    }

    override fun toString(): String {
        return "$game: $title $part ($videoId)"
    }
    override fun compareTo(other: Detail): Int {
        if (other.dateUploaded.value == dateUploaded.value) {
            //Todo: This is weird and shouldn't happen. Maybe we're getting the wrong dateUploaded
            //println("equal")
        }
        return dateUploaded.value.compareTo(other.dateUploaded.value)
    }

    companion object {
    }
}
