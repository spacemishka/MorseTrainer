package com.spacemishka.app.morsetrainer.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "user_statistics",
    foreignKeys = [
        ForeignKey(
            entity = UserProfile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId", "character"], unique = true)]
)
data class UserStatistic(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val character: String,
    val attempts: Int = 0,
    val correct: Int = 0,
    val lastAccuracy: Float = 0f
)
