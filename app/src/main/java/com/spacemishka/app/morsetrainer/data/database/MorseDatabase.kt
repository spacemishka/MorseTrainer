package com.spacemishka.app.morsetrainer.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.spacemishka.app.morsetrainer.data.dao.AchievementDao
import com.spacemishka.app.morsetrainer.data.dao.UserProfileDao
import com.spacemishka.app.morsetrainer.data.dao.UserStatisticDao
import com.spacemishka.app.morsetrainer.data.entity.Achievement
import com.spacemishka.app.morsetrainer.data.entity.UserProfile
import com.spacemishka.app.morsetrainer.data.entity.UserStatistic

@Database(
    entities = [UserProfile::class, UserStatistic::class, Achievement::class],
    version = 4,
    exportSchema = false
)
abstract class MorseDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun userStatisticDao(): UserStatisticDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: MorseDatabase? = null

        fun getDatabase(context: Context): MorseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MorseDatabase::class.java,
                    "morse_trainer_database"
                )
                .fallbackToDestructiveMigration() // Simple strategy for initial versions
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
