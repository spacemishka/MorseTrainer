package com.spacemishka.app.morsetrainer

import android.os.Bundle
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.spacemishka.app.morsetrainer.ui.*
import com.spacemishka.app.morsetrainer.ui.theme.MorseTrainerTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MorseViewModel by viewModels()
    private val hangmanViewModel: HangmanViewModel by viewModels()
    private val treasureHuntViewModel: TreasureHuntViewModel by viewModels()
    private val typingChallengeViewModel: TypingChallengeViewModel by viewModels()
    private val memoryViewModel: MemoryViewModel by viewModels()
    private val qsoSimulatorViewModel: QsoSimulatorViewModel by viewModels()

    private var appliedLanguage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve and apply the preferred language synchronously before setContent to prevent screen flashing
        val prefs = getSharedPreferences("morse_trainer_prefs", MODE_PRIVATE)
        val startLang = prefs.getString("current_applied_lang", "SYSTEM") ?: "SYSTEM"
        applyLocale(startLang)
        appliedLanguage = startLang

        enableEdgeToEdge()
        setContent {
            MorseTrainerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color(0xFF0F172A)
                ) {
                    val navController = rememberNavController()

                    // Observe current profile language override
                    val profile by viewModel.currentProfile.collectAsState()
                    LaunchedEffect(profile) {
                        val activePrefs = getSharedPreferences("morse_trainer_prefs", MODE_PRIVATE)
                        val activeProfileId = activePrefs.getLong("active_profile_id", -1L)
                        if (profile != null) {
                            val targetLang = profile!!.preferredLanguage
                            if (targetLang != appliedLanguage) {
                                activePrefs.edit().putString("current_applied_lang", targetLang).apply()
                                applyLocale(targetLang)
                                appliedLanguage = targetLang
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                    recreate()
                                }
                            }
                        } else if (activeProfileId == -1L) {
                            val targetLang = "SYSTEM"
                            if (targetLang != appliedLanguage) {
                                activePrefs.edit().putString("current_applied_lang", targetLang).apply()
                                applyLocale(targetLang)
                                appliedLanguage = targetLang
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                    recreate()
                                }
                            }
                        }
                    }

                    NavGraph(
                        navController = navController,
                        viewModel = viewModel,
                        hangmanViewModel = hangmanViewModel,
                        treasureHuntViewModel = treasureHuntViewModel,
                        typingChallengeViewModel = typingChallengeViewModel,
                        memoryViewModel = memoryViewModel,
                        qsoSimulatorViewModel = qsoSimulatorViewModel
                    )
                }
            }
        }
    }

    private fun applyLocale(languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = getSystemService(LocaleManager::class.java)
            val localeList = if (languageCode == "SYSTEM") {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(languageCode)
            }
            localeManager.applicationLocales = localeList
        } else {
            val targetLocale = if (languageCode == "SYSTEM") {
                android.content.res.Resources.getSystem().configuration.locales.get(0)
            } else {
                Locale(languageCode)
            }
            Locale.setDefault(targetLocale)

            val resources = this.resources
            val configuration = resources.configuration
            configuration.setLocale(targetLocale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)

            val appResources = applicationContext.resources
            val appConfig = appResources.configuration
            appConfig.setLocale(targetLocale)
            @Suppress("DEPRECATION")
            appResources.updateConfiguration(appConfig, appResources.displayMetrics)
        }
    }
}