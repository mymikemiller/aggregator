package com.mymikemiller.gamegrumpsplayer

import org.junit.Test

import org.junit.Assert.*

/**

 */
class DetailsTest {
    @Test
    fun first_details_areParsedCorrectly() {
        val d = Details(
                "Kirby Super Star - Spring Breeze ADVENTURES! - GameGrumps",
                "", "")
        assertEquals("Kirby Super Star", d.game)
        assertEquals("Spring Breeze ADVENTURES!", d.title)
        assertEquals("", d.part)
        assertEquals("GameGrumps", d.team)
    }

    @Test
    fun old_details_areParsedCorrectly() {
        val d = Details(
                "Mega Man 7 - This Game Rocks... Man - Part 1 - Game Grumps",
                "", "")
        assertEquals("Mega Man 7", d.game)
        assertEquals("This Game Rocks... Man", d.title)
        assertEquals("Part 1", d.part)
        assertEquals("Game Grumps", d.team)
    }

    @Test
    fun new_details_areParsedCorrectly() {
        val d = Details(
                "Zelda's Adventure: Vile Blue Vile - PART 6 - Game Grumps",
                "", "")
        assertEquals("Zelda's Adventure", d.game)
        assertEquals( "Vile Blue Vile", d.title)
        assertEquals("PART 6", d.part)
        assertEquals("Game Grumps", d.team)
    }

}