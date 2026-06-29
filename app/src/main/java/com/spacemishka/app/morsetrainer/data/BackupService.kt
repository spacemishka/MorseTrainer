package com.spacemishka.app.morsetrainer.data

import com.spacemishka.app.morsetrainer.data.database.MorseDatabase
import com.spacemishka.app.morsetrainer.data.entity.Achievement
import com.spacemishka.app.morsetrainer.data.entity.UserProfile
import com.spacemishka.app.morsetrainer.data.entity.UserStatistic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupData(
    val profiles: List<UserProfile>,
    val statistics: List<UserStatistic>,
    val achievements: List<Achievement>
)

class BackupService(private val database: MorseDatabase) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        val profiles = database.userProfileDao().getAllProfiles().first()
        
        val allStatistics = mutableListOf<UserStatistic>()
        val allAchievements = mutableListOf<Achievement>()

        for (profile in profiles) {
            val stats = database.userStatisticDao().getStatisticsForProfile(profile.id).first()
            val achievements = database.achievementDao().getAchievementsForProfile(profile.id).first()
            allStatistics.addAll(stats)
            allAchievements.addAll(achievements)
        }

        val backup = BackupData(
            profiles = profiles,
            statistics = allStatistics,
            achievements = allAchievements
        )
        json.encodeToString(backup)
    }

    suspend fun importBackup(backupJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backup = json.decodeFromString<BackupData>(backupJson)
            
            // For each profile in the backup, we will insert it, get its new ID, 
            // and remap the statistics and achievements so they match the new profile ID.
            for (profile in backup.profiles) {
                val cleanProfile = profile.copy(id = 0) // Room will generate a new ID
                val newProfileId = database.userProfileDao().insertProfile(cleanProfile)

                // Filter statistics that belonged to the old profile ID
                val profileStats = backup.statistics.filter { it.profileId == profile.id }
                for (stat in profileStats) {
                    val cleanStat = stat.copy(id = 0, profileId = newProfileId)
                    database.userStatisticDao().insertStatistic(cleanStat)
                }

                // Filter achievements that belonged to the old profile ID
                val profileAchievements = backup.achievements.filter { it.profileId == profile.id }
                for (achievement in profileAchievements) {
                    val cleanAchievement = achievement.copy(id = 0, profileId = newProfileId)
                    database.achievementDao().insertAchievement(cleanAchievement)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
