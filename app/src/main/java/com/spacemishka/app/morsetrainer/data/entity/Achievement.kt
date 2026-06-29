package com.spacemishka.app.morsetrainer.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "achievements",
    foreignKeys = [
        ForeignKey(
            entity = UserProfile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId", "achievementKey"], unique = true)]
)
data class Achievement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val achievementKey: String,
    val unlockedTimestamp: Long = System.currentTimeMillis()
)
