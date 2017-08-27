package com.mymikemiller.gamegrumpsplayer

import com.google.api.client.util.DateTime
import com.mymikemiller.gamegrumpsplayer.util.PlaylistManipulator
import org.junit.Assert
import org.junit.Test

/**
 *
 */

class PlaylistManipulatorTest {


    @Test
    fun orderByDateUploadedIsCorrect() {
        val input: List<Detail> = generateTestInput()

        val expectedOutput = listOf<Detail>(
                Detail("0", "Zelda's Adventure: Vile Blue Vile - PART 0 - Game Grumps","", "", DateTime(0)),
                Detail("1", "Kirby's Adventure: Vile Blue Vile - PART 0 - Game Grumps","", "", DateTime(0)),
                Detail("2", "Zelda's Adventure: Vile Blue Vile - PART 1 - Game Grumps","", "", DateTime(0)),
                Detail("3", "Kirby's Adventure: Vile Blue Vile - PART 1 - Game Grumps","", "", DateTime(0)),
                Detail("4", "Zelda's Adventure: Vile Blue Vile - PART 2 - Game Grumps","", "", DateTime(0)),
                Detail("5", "Kirby's Adventure: Vile Blue Vile - PART 2 - Game Grumps","", "", DateTime(0))
        )

        val output:List<Detail> = PlaylistManipulator.orderByDate(input)

        // contentEquals isn't there, so we'll do it ourselves.
        Assert.assertEquals(output[0], expectedOutput[0])
        Assert.assertEquals(output[1], expectedOutput[1])
        Assert.assertEquals(output[2], expectedOutput[2])
        Assert.assertEquals(output[3], expectedOutput[3])
        Assert.assertEquals(output[4], expectedOutput[4])
        Assert.assertEquals(output[5], expectedOutput[5])
    }

    @Test
    fun orderByGameIsCorrect() {
        val input: List<Detail> = generateTestInput()

        val expectedOutput = listOf<Detail>(
                Detail("0", "Zelda's Adventure: Vile Blue Vile - PART 0 - Game Grumps","", "", DateTime(0)),
                Detail("2", "Zelda's Adventure: Vile Blue Vile - PART 1 - Game Grumps","", "", DateTime(0)),
                Detail("4", "Zelda's Adventure: Vile Blue Vile - PART 2 - Game Grumps","", "", DateTime(0)),
                Detail("1", "Kirby's Adventure: Vile Blue Vile - PART 0 - Game Grumps","", "", DateTime(0)),
                Detail("3", "Kirby's Adventure: Vile Blue Vile - PART 1 - Game Grumps","", "", DateTime(0)),
                Detail("5", "Kirby's Adventure: Vile Blue Vile - PART 2 - Game Grumps","", "", DateTime(0))
        )

        val output:List<Detail> = PlaylistManipulator.orderByGame(input)

        // contentEquals isn't there, so we'll do it ourselves.

        Assert.assertEquals(output[0], expectedOutput[0])
        Assert.assertEquals(output[1], expectedOutput[1])
        Assert.assertEquals(output[2], expectedOutput[2])
        Assert.assertEquals(output[3], expectedOutput[3])
        Assert.assertEquals(output[4], expectedOutput[4])
        Assert.assertEquals(output[5], expectedOutput[5])
    }
}

fun generateTestInput(): List<Detail> {

    val input = listOf<Detail>(
            Detail("0", "Zelda's Adventure: Vile Blue Vile - PART 0 - Game Grumps","", "", DateTime(0)),
            Detail("1", "Kirby's Adventure: Vile Blue Vile - PART 0 - Game Grumps","", "", DateTime(0)),
            Detail("2", "Zelda's Adventure: Vile Blue Vile - PART 1 - Game Grumps","", "", DateTime(0)),
            Detail("3", "Kirby's Adventure: Vile Blue Vile - PART 1 - Game Grumps","", "", DateTime(0)),
            Detail("4", "Zelda's Adventure: Vile Blue Vile - PART 2 - Game Grumps","", "", DateTime(0)),
            Detail("5", "Kirby's Adventure: Vile Blue Vile - PART 2 - Game Grumps","", "", DateTime(0))
    )
    return input
}
