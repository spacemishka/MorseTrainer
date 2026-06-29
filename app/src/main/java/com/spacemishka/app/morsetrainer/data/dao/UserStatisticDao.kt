package com.spacemishka.app.morsetrainer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spacemishka.app.morsetrainer.data.entity.UserStatistic
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatisticDao {
    @Query("SELECT * FROM user_statistics WHERE profileId = :profileId")
    fun getStatisticsForProfile(profileId: Long): Flow<List<UserStatistic>>

    @Query("SELECT * FROM user_statistics WHERE profileId = :profileId AND character = :character LIMIT 1")
    fun getStatisticForCharacter(profileId: Long, character: String): UserStatistic?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStatistic(statistic: UserStatistic): Long

    @Update
    fun updateStatistic(statistic: UserStatistic)

    @Query("DELETE FROM user_statistics WHERE profileId = :profileId")
    fun deleteStatisticsForProfile(profileId: Long)
}
