package com.spacemishka.app.morsetrainer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spacemishka.app.morsetrainer.audio.MorseAudioEngine
import com.spacemishka.app.morsetrainer.data.BackupService
import com.spacemishka.app.morsetrainer.data.database.MorseDatabase
import com.spacemishka.app.morsetrainer.data.entity.Achievement
import com.spacemishka.app.morsetrainer.data.entity.UserProfile
import com.spacemishka.app.morsetrainer.data.entity.UserStatistic
import com.spacemishka.app.morsetrainer.domain.KochProgressionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MorseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MorseDatabase.getDatabase(application)
    private val audioEngine = MorseAudioEngine()
    private val kochEngine = KochProgressionEngine()

    init {
        val prefs = application.getSharedPreferences("morse_trainer_prefs", android.content.Context.MODE_PRIVATE)
        val activeProfileId = prefs.getLong("active_profile_id", -1L)
        if (activeProfileId != -1L) {
            viewModelScope.launch {
                val profile = withContext(Dispatchers.IO) {
                    db.userProfileDao().getProfileById(activeProfileId)
                }
                if (profile != null) {
                    withContext(Dispatchers.Main) {
                        selectProfile(profile)
                    }
                }
            }
        }
    }

    // All profiles
    val allProfiles: StateFlow<List<UserProfile>> = db.userProfileDao().getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Current profile
    private val _currentProfile = MutableStateFlow<UserProfile?>(null)
    val currentProfile: StateFlow<UserProfile?> = _currentProfile.asStateFlow()

    // Current profile's stats
    val currentStats: StateFlow<List<UserStatistic>> = _currentProfile.flatMapLatest { profile ->
        profile?.let { db.userStatisticDao().getStatisticsForProfile(it.id) } ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Current profile's achievements
    val currentAchievements: StateFlow<List<Achievement>> = _currentProfile.flatMapLatest { profile ->
        profile?.let { db.achievementDao().getAchievementsForProfile(it.id) } ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Lesson States
    private val _currentTargetChar = MutableStateFlow("")
    val currentTargetChar: StateFlow<String> = _currentTargetChar.asStateFlow()

    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio: StateFlow<Boolean> = _isPlayingAudio.asStateFlow()

    private val _lastInputFeedback = MutableStateFlow<Boolean?>(null) // true if correct, false if wrong, null if pending
    val lastInputFeedback: StateFlow<Boolean?> = _lastInputFeedback.asStateFlow()

    private val _recentAttempts = MutableStateFlow<List<Boolean>>(emptyList())
    val recentAttempts: StateFlow<List<Boolean>> = _recentAttempts.asStateFlow()

    private val _sessionTargets = MutableStateFlow<List<String>>(emptyList())
    val sessionTargets: StateFlow<List<String>> = _sessionTargets.asStateFlow()

    private val _lessonAccuracy = MutableStateFlow(0f)
    val lessonAccuracy: StateFlow<Float> = _lessonAccuracy.asStateFlow()

    private val _unlockedPromotion = MutableStateFlow(false)
    val unlockedPromotion: StateFlow<Boolean> = _unlockedPromotion.asStateFlow()

    fun selectProfile(profile: UserProfile?) {
        _currentProfile.value = profile
        val prefs = getApplication<Application>().getSharedPreferences("morse_trainer_prefs", android.content.Context.MODE_PRIVATE)
        if (profile != null) {
            prefs.edit().putLong("active_profile_id", profile.id).apply()
            audioEngine.toneFrequencyHz = profile.toneFrequencyHz
            audioEngine.volumePercent = profile.volume
            audioEngine.speedWpm = profile.speedWpm
            audioEngine.effectiveSpeedWpm = profile.effectiveSpeedWpm
            audioEngine.audioProfile = profile.audioProfile
            
            // Generate first target character after resetting stats
            resetSessionStats()
            generateNextTarget()
        } else {
            prefs.edit().remove("active_profile_id").apply()
        }
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            val newProfile = UserProfile(name = name)
            val newId = withContext(Dispatchers.IO) {
                db.userProfileDao().insertProfile(newProfile)
            }
            val inserted = withContext(Dispatchers.IO) {
                db.userProfileDao().getProfileById(newId)
            }
            if (_currentProfile.value == null) {
                selectProfile(inserted)
            }
        }
    }

    fun updateProfileSettings(
        wpm: Int,
        effectiveWpm: Int,
        frequency: Int,
        volume: Int,
        audioProfile: String,
        preferredLanguage: String,
        kochWindowSize: Int
    ) {
        val current = _currentProfile.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                speedWpm = wpm,
                effectiveSpeedWpm = effectiveWpm,
                toneFrequencyHz = frequency,
                volume = volume,
                audioProfile = audioProfile,
                preferredLanguage = preferredLanguage,
                kochWindowSize = kochWindowSize
            )
            withContext(Dispatchers.IO) {
                db.userProfileDao().updateProfile(updated)
            }
            _currentProfile.value = updated

            audioEngine.speedWpm = wpm
            audioEngine.effectiveSpeedWpm = effectiveWpm
            audioEngine.toneFrequencyHz = frequency
            audioEngine.volumePercent = volume
            audioEngine.audioProfile = audioProfile
        }
    }

    fun deleteProfile(profile: UserProfile) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.userProfileDao().deleteProfile(profile)
            }
            if (_currentProfile.value?.id == profile.id) {
                _currentProfile.value = null
            }
        }
    }

    fun resetSessionStats() {
        _recentAttempts.value = emptyList()
        _sessionTargets.value = emptyList()
        _lessonAccuracy.value = 0f
        _unlockedPromotion.value = false
        _lastInputFeedback.value = null
    }

    // Audio Player triggers
    fun playCurrentTarget() {
        val target = _currentTargetChar.value
        if (target.isEmpty()) return
        _isPlayingAudio.value = true
        audioEngine.playText(target) {
            _isPlayingAudio.value = false
        }
    }

    fun playTextDirectly(text: String) {
        _isPlayingAudio.value = true
        audioEngine.playText(text) {
            _isPlayingAudio.value = false
        }
    }

    fun stopAudio() {
        audioEngine.stop()
        _isPlayingAudio.value = false
    }

    // Koch lesson progression
    fun generateNextTarget() {
        val profile = _currentProfile.value ?: return
        val activeChars = kochEngine.getActiveCharacters(profile.currentKochLevel, profile.customSequence)
        val nextChar = kochEngine.selectNextCharacter(activeChars, currentStats.value, _sessionTargets.value)
        _currentTargetChar.value = nextChar
        _lastInputFeedback.value = null

        // Add to session targets
        val targets = _sessionTargets.value.toMutableList()
        targets.add(nextChar)
        _sessionTargets.value = targets
    }

    fun handleUserInput(input: String) {
        val target = _currentTargetChar.value
        if (target.isEmpty() || input.isEmpty()) return

        val isCorrect = input.trim().uppercase() == target.uppercase()
        _lastInputFeedback.value = isCorrect

        // Add to recent session attempts
        val attempts = _recentAttempts.value.toMutableList()
        attempts.add(isCorrect)
        _recentAttempts.value = attempts

        // Calculate session accuracy
        val correctCount = attempts.count { it }
        _lessonAccuracy.value = correctCount.toFloat() / attempts.size

        // Record in Database
        saveCharacterAttempt(target, isCorrect)

        // Verify promotion
        val profile = _currentProfile.value
        if (profile != null && kochEngine.shouldPromote(attempts, profile.kochWindowSize)) {
            _unlockedPromotion.value = true
        }

        // Trigger milestone achievements
        checkAndUnlockAchievements(attempts.size, isCorrect)
    }

    fun promoteUser() {
        val profile = _currentProfile.value ?: return
        val nextLevel = profile.currentKochLevel + 1
        val maxLevel = profile.customSequence.length - 1
        if (nextLevel <= maxLevel) {
            viewModelScope.launch {
                val updated = profile.copy(currentKochLevel = nextLevel)
                withContext(Dispatchers.IO) {
                    db.userProfileDao().updateProfile(updated)
                }
                _currentProfile.value = updated
                
                // Add Koch Course Completion achievement if level reaches max
                if (nextLevel == maxLevel) {
                    unlockAchievement("KOCH_COMPLETED")
                }
                
                resetSessionStats()
                generateNextTarget()
            }
        }
    }

    private fun saveCharacterAttempt(char: String, correct: Boolean) {
        val profileId = _currentProfile.value?.id ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val stat = db.userStatisticDao().getStatisticForCharacter(profileId, char)
                if (stat == null) {
                    db.userStatisticDao().insertStatistic(
                        UserStatistic(
                            profileId = profileId,
                            character = char,
                            attempts = 1,
                            correct = if (correct) 1 else 0,
                            lastAccuracy = if (correct) 1.0f else 0.0f
                        )
                    )
                } else {
                    val newAttempts = stat.attempts + 1
                    val newCorrect = stat.correct + if (correct) 1 else 0
                    val newAccuracy = newCorrect.toFloat() / newAttempts
                    db.userStatisticDao().updateStatistic(
                        stat.copy(
                            attempts = newAttempts,
                            correct = newCorrect,
                            lastAccuracy = newAccuracy
                        )
                    )
                }
            }
        }
    }

    private fun checkAndUnlockAchievements(sessionAttempts: Int, lastCorrect: Boolean) {
        val profile = _currentProfile.value ?: return
        viewModelScope.launch {
            // "First lesson completed" (10 attempts inside one session)
            if (sessionAttempts >= 10) {
                unlockAchievement("FIRST_LESSON_COMPLETED")
            }

            // Check total attempts across all characters
            val totalAttempts = currentStats.value.sumOf { it.attempts }
            if (totalAttempts >= 1000) {
                unlockAchievement("CHARACTERS_1000")
            }
        }
    }

    fun unlockAchievement(key: String) {
        val profileId = _currentProfile.value?.id ?: return
        viewModelScope.launch {
            val alreadyUnlocked = withContext(Dispatchers.IO) {
                db.achievementDao().hasAchievement(profileId, key)
            }
            if (!alreadyUnlocked) {
                withContext(Dispatchers.IO) {
                    db.achievementDao().insertAchievement(
                        Achievement(profileId = profileId, achievementKey = key)
                    )
                }
            }
        }
    }

    // JSON Backup / Restore triggers
    suspend fun exportDataJson(): String {
        return BackupService(db).exportBackup()
    }

    suspend fun importDataJson(jsonString: String): Boolean {
        val success = BackupService(db).importBackup(jsonString)
        if (success) {
            // Refresh profiles list
            // (handled reactively by Flow)
        }
        return success
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
