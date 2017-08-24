package com.mymikemiller.gamegrumpsplayer

import com.google.api.client.util.DateTime
import org.junit.Test

import org.junit.Assert.*

/**

 */
class DetailTest {
    @Test
    fun first_detail_isParsedCorrectly() {
        val d = Detail(
                "xxx",
                "Kirby Super Star - Spring Breeze ADVENTURES! - GameGrumps",
                "", "", DateTime(0))
        assertEquals("Kirby Super Star", d.game)
        assertEquals("Spring Breeze ADVENTURES!", d.title)
        assertEquals("", d.part)
        assertEquals("GameGrumps", d.team)
    }

    @Test
    fun old_detail_isParsedCorrectly() {
        val d = Detail(
                "xxx",
                "Mega Man 7 - This Game Rocks... Man - Part 1 - Game Grumps",
                "", "", DateTime(0))
        assertEquals("Mega Man 7", d.game)
        assertEquals("This Game Rocks... Man", d.title)
        assertEquals("Part 1", d.part)
        assertEquals("Game Grumps", d.team)
    }

    @Test
    fun new_detail_isParsedCorrectly() {
        val d = Detail(
                "xxx",
                "Zelda's Adventure: Vile Blue Vile - PART 6 - Game Grumps",
                "", "", DateTime(0))
        assertEquals("Zelda's Adventure", d.game)
        assertEquals( "Vile Blue Vile", d.title)
        assertEquals("PART 6", d.part)
        assertEquals("Game Grumps", d.team)
    }
    @Test
    fun funny_mii_thing_detail_isParsedCorrectly() {
        val d = Detail(
                "xxx",
                "Funny mii thing",
                "", "", DateTime(0))
        assertEquals("Funny mii thing", d.game)
        assertEquals("Funny mii thing", d.title)
        assertEquals("", d.part)
        assertEquals("Funny mii thing", d.team)
    }

    @Test
    fun finale_detail_isParsedCorrectly() {
        val d = Detail(
                "xxx",
                "Joe & Mac: Finale - Game Grums",
                "", "", DateTime(0))
        assertEquals("Joe & Mac", d.game)
        assertEquals("Finale", d.title)
        assertEquals("", d.part)
        assertEquals("Funny mii thing", d.team)
    }

}