package com.spacemishka.app.morsetrainer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spacemishka.app.morsetrainer.data.entity.Achievement
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE profileId = :profileId")
    fun getAchievementsForProfile(profileId: Long): Flow<List<Achievement>>

    @Query("SELECT COUNT(*) > 0 FROM achievements WHERE profileId = :profileId AND achievementKey = :key LIMIT 1")
    fun hasAchievement(profileId: Long, key: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAchievement(achievement: Achievement): Long

    @Query("DELETE FROM achievements WHERE profileId = :profileId")
    fun deleteAchievementsForProfile(profileId: Long)
}
