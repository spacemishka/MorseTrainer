package com.spacemishka.app.morsetrainer

import com.spacemishka.app.morsetrainer.data.entity.UserStatistic
import com.spacemishka.app.morsetrainer.domain.KochProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KochProgressionEngineTest {

    private val engine = KochProgressionEngine()

    @Test
    fun testGetActiveCharacters() {
        val chars = engine.getActiveCharacters(1)
        assertEquals(2, chars.size)
        assertEquals("K", chars[0])
        assertEquals("M", chars[1])

        val chars5 = engine.getActiveCharacters(4)
        assertEquals(5, chars5.size)
        assertEquals("K", chars5[0])
        assertEquals("M", chars5[1])
        assertEquals("R", chars5[2])
        assertEquals("S", chars5[3])
        assertEquals("U", chars5[4])
    }

    @Test
    fun testShouldPromote() {
        // Less than minimum window size (20)
        val shortList = List(15) { true }
        assertFalse(engine.shouldPromote(shortList))

        // exactly 90% correct
        val perfectList = List(20) { true }
        assertTrue(engine.shouldPromote(perfectList))

        // exactly 18 correct out of 20 (90%)
        val boundaryList = MutableList(20) { true }
        boundaryList[0] = false
        boundaryList[1] = false
        assertTrue(engine.shouldPromote(boundaryList))

        // 17 correct out of 20 (85%)
        val failList = MutableList(20) { true }
        failList[0] = false
        failList[1] = false
        failList[2] = false
        assertFalse(engine.shouldPromote(failList))
    }

    @Test
    fun testSelectNextCharacterAdaptiveRepetition() {
        val activeChars = listOf("K", "M")
        
        // Scenario A: Both characters have equal stats (100% correct). Selection should be distributed.
        val statsEqual = listOf(
            UserStatistic(profileId = 1, character = "K", attempts = 10, correct = 10, lastAccuracy = 1.0f),
            UserStatistic(profileId = 1, character = "M", attempts = 10, correct = 10, lastAccuracy = 1.0f)
        )
        
        // Run a simulation of 100 choices and ensure both are chosen
        val selectedA = mutableMapOf<String, Int>()
        for (i in 0 until 100) {
            val choice = engine.selectNextCharacter(activeChars, statsEqual)
            selectedA[choice] = (selectedA[choice] ?: 0) + 1
        }
        assertTrue(selectedA.containsKey("K"))
        assertTrue(selectedA.containsKey("M"))

        // Scenario B: "K" has 100% accuracy, "M" has 40% accuracy (problem character).
        // Adaptive algorithm should favor M.
        val statsSkewed = listOf(
            UserStatistic(profileId = 1, character = "K", attempts = 10, correct = 10, lastAccuracy = 1.0f),
            UserStatistic(profileId = 1, character = "M", attempts = 10, correct = 4, lastAccuracy = 0.4f)
        )
        val selectedB = mutableMapOf<String, Int>()
        for (i in 0 until 1000) {
            val choice = engine.selectNextCharacter(activeChars, statsSkewed)
            selectedB[choice] = (selectedB[choice] ?: 0) + 1
        }
        val countK = selectedB["K"] ?: 0
        val countM = selectedB["M"] ?: 0
        
        // "M" should be selected significantly more often than "K"
        assertTrue("Expected M count ($countM) to be greater than K count ($countK)", countM > countK)
    }

    @Test
    fun testSelectNextCharacterEnforces40PercentNewCharacter() {
        val activeChars = listOf("K", "M", "R") // 'R' is the new character
        val statistics = emptyList<UserStatistic>()
        
        // Scenario A: sessionTargets is empty. Should return the new character 'R'.
        val next1 = engine.selectNextCharacter(activeChars, statistics, emptyList())
        assertEquals("R", next1)

        // Scenario B: sessionTargets has 5 elements, 1 'R' (20%). Under 40%, so should return 'R'.
        val targetsLow = listOf("K", "M", "R", "K", "M")
        val next2 = engine.selectNextCharacter(activeChars, statistics, targetsLow)
        assertEquals("R", next2)

        // Scenario C: sessionTargets has 5 elements, 2 'R' (40%). 40% threshold reached, should allow random selection.
        val targetsGood = listOf("K", "M", "R", "K", "R")
        // Since statistics is empty, unpracticed characters have equal weight.
        // Let's run a simulation of 100 times, and verify it doesn't only choose 'R' (e.g. it selects 'K' or 'M' as well).
        var selectedR = 0
        var selectedOthers = 0
        for (i in 0 until 100) {
            val choice = engine.selectNextCharacter(activeChars, statistics, targetsGood)
            if (choice == "R") {
                selectedR++
            } else {
                selectedOthers++
            }
        }
        // It should have selected others at least once in 100 trials.
        assertTrue(selectedOthers > 0)
    }

    @Test
    fun testSelectNextCharacterPreventsConsecutiveRepeats() {
        val activeChars = listOf("K", "M", "R")
        val statistics = emptyList<UserStatistic>()
        
        // With "R" at 66% frequency, the 40% rule is satisfied, so it should avoid repeating "R" consecutively
        val sessionTargets = listOf("K", "R", "R")
        for (i in 0 until 100) {
            val choice = engine.selectNextCharacter(activeChars, statistics, sessionTargets)
            assertTrue(choice != "R")
        }
    }
}
