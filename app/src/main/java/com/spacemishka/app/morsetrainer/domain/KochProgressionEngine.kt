package com.spacemishka.app.morsetrainer.domain

import com.spacemishka.app.morsetrainer.data.entity.UserProfile
import com.spacemishka.app.morsetrainer.data.entity.UserStatistic
import kotlin.random.Random

class KochProgressionEngine {

    companion object {
        const val PROMOTION_THRESHOLD = 0.90f // 90%
        const val MIN_WINDOW_SIZE = 20 // Number of recent attempts to verify promotion
    }

    /**
     * Get list of characters unlocked at the user's current Koch level.
     */
    fun getActiveCharacters(level: Int, customSequence: String? = null): List<String> {
        val sequence = customSequence?.takeIf { it.isNotEmpty() } ?: UserProfile.DEFAULT_KOCH_SEQUENCE
        val end = (level + 1).coerceIn(2, sequence.length)
        return sequence.substring(0, end).map { it.toString() }
    }

    /**
     * Evaluates whether a user should be promoted.
     * Takes the list of recent attempts (true for correct, false for incorrect).
     * If the recent accuracy in the window (min MIN_WINDOW_SIZE) is >= 90%, return true.
     */
    fun shouldPromote(recentAttempts: List<Boolean>, windowSize: Int = MIN_WINDOW_SIZE): Boolean {
        if (recentAttempts.size < windowSize) return false
        val window = recentAttempts.takeLast(windowSize)
        val correctCount = window.count { it }
        val accuracy = correctCount.toFloat() / windowSize
        return accuracy >= PROMOTION_THRESHOLD
    }

    /**
     * Selects the next character for the user to practice, using adaptive weighting.
     * Unlocked characters with lower accuracy or fewer attempts will have a higher probability of selection.
     */
    fun selectNextCharacter(
        activeCharacters: List<String>,
        statistics: List<UserStatistic>,
        sessionTargets: List<String>? = null
    ): String {
        if (activeCharacters.isEmpty()) return "K" // Fallback

        if (sessionTargets != null) {
            // Enforce the 40% target representation rule for the newly introduced character.
            // The new character is always the last element of activeCharacters.
            val newChar = activeCharacters.last()
            val totalAttempts = sessionTargets.size
            if (totalAttempts > 0) {
                val newCharCount = sessionTargets.count { it == newChar }
                val percentage = newCharCount.toDouble() / totalAttempts
                if (percentage < 0.40) {
                    return newChar
                }
            } else {
                // First attempt of the lesson level must introduce the new character
                return newChar
            }
        }

        val lastTarget = sessionTargets?.lastOrNull()
        val statMap = statistics.associateBy { it.character }
        val weights = DoubleArray(activeCharacters.size)
        var totalWeight = 0.0

        for (i in activeCharacters.indices) {
            val char = activeCharacters[i]
            if (activeCharacters.size > 1 && char == lastTarget) {
                weights[i] = 0.0
                continue
            }

            val stat = statMap[char]
            val weight = if (stat == null || stat.attempts < 5) {
                // High weight for new or rarely practiced characters
                4.0
            } else {
                val accuracy = if (stat.attempts > 0) {
                    stat.correct.toDouble() / stat.attempts
                } else {
                    1.0
                }

                if (accuracy >= 0.90) {
                    // Well-known character, standard weight
                    1.0
                } else {
                    // Low accuracy characters get prioritized.
                    // E.g., if accuracy is 60%, error is 40%. Weight = 1.0 + 0.40 * 6.0 = 3.4
                    val errorRate = 1.0 - accuracy
                    1.0 + errorRate * 6.0
                }
            }

            weights[i] = weight
            totalWeight += weight
        }

        // Fallback if totalWeight is 0.0
        if (totalWeight <= 0.0) {
            val remaining = activeCharacters.filter { it != lastTarget }
            return if (remaining.isNotEmpty()) remaining.random() else activeCharacters.random()
        }

        // Weighted random selection
        val randomValue = Random.nextDouble() * totalWeight
        var cumulativeWeight = 0.0
        for (i in activeCharacters.indices) {
            cumulativeWeight += weights[i]
            if (randomValue <= cumulativeWeight) {
                return activeCharacters[i]
            }
        }

        return activeCharacters.last() // Fallback to last character
    }
}
