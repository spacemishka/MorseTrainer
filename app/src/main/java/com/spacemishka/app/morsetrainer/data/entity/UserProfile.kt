package com.spacemishka.app.morsetrainer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currentKochLevel: Int = 1, // Starts with 2 characters (e.g. K, M)
    val speedWpm: Int = 20,
    val effectiveSpeedWpm: Int = 10,
    val toneFrequencyHz: Int = 600,
    val volume: Int = 80, // 0 to 100
    val customSequence: String = DEFAULT_KOCH_SEQUENCE,
    val audioProfile: String = "SINE", // "SINE", "NOISY", "FADING"
    val preferredLanguage: String = "SYSTEM", // "SYSTEM", "en", "de", "fr", "es"
    val kochWindowSize: Int = 20
) {
    companion object {
        const val DEFAULT_KOCH_SEQUENCE = "KMRSUAPTLOWI.NJEF0YVG5Q9ZH38B?47C1D6X/=2"
    }
}
