# Override System Default Language on Demand

This plan details how to add a language selection setting to the Morse Trainer application. Users will be able to override the system locale with English, German, French, or Spanish, persisting the selection per profile.

## User Review Required

> [!IMPORTANT]
> **Database Version Bump:**
> * We will add a `preferredLanguage` column to the `UserProfile` database entity.
> * The `MorseDatabase` schema version will be bumped from 1 to 2. Since `fallbackToDestructiveMigration()` is enabled, database recreation is handled automatically.

> [!TIP]
> **State Management & Recreation:**
> * Changing locales dynamically at runtime requires recreating the `Activity` so all local resources (such as standard layout menus and headers) compile with the updated locale configuration.
> * To prevent losing the active profile upon recreation, we will persist the selected profile's ID in `SharedPreferences`.

## Open Questions

> [!NOTE]
> None at this stage. Storing the active profile and applied language globally in `SharedPreferences` allows smooth recreation and launch.

---

## Proposed Changes

### Component 1: Data Access Layer (Room DB)

#### [MODIFY] [UserProfile.kt](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/data/entity/UserProfile.kt)
* Add a `preferredLanguage` parameter to the entity class with a default value of `"SYSTEM"`.

#### [MODIFY] [MorseDatabase.kt](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/data/database/MorseDatabase.kt)
* Increment database version to `2`.

---

### Component 2: ViewModel Orchestration

#### [MODIFY] [MorseViewModel.kt](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/MorseViewModel.kt)
* Update `selectProfile(profile)` to write the active profile ID to `SharedPreferences`.
* Add an auto-select lookup on initialization to resume the active profile after configuration recreation.
* Update `updateProfileSettings` to accept and write the `preferredLanguage` setting.

---

### Component 3: Activity Lifecycle & Context Wrapping

#### [MODIFY] [MainActivity.kt](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/MainActivity.kt)
* Load and apply the saved preferred language synchronously in `onCreate` before `setContent` to avoid screen flashing.
* Use a Compose side effect (`LaunchedEffect`) to detect when the profile's preferred language changes, update configurations, and trigger `recreate()`.

---

### Component 4: Settings Menu UI & Localization Resources

#### [MODIFY] [MorseAppScreens.kt](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/screens/MorseAppScreens.kt)
* Add a Language selector card in the `SettingsScreen` UI.
* Use a dropdown menu listing the options: System Default, English, German, French, and Spanish.

#### [MODIFY] [strings.xml (en)](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/res/values/strings.xml)
* Add translations keys: `language_settings`, `lang_system`, `lang_en`, `lang_de`, `lang_fr`, `lang_es`.

#### [MODIFY] [strings.xml (de)](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/res/values-de/strings.xml)
* Add translation keys in German.

#### [MODIFY] [strings.xml (fr)](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/res/values-fr/strings.xml)
* Add translation keys in French.

#### [MODIFY] [strings.xml (es)](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/res/values-es/strings.xml)
* Add translation keys in Spanish.

---

## Verification Plan

### Automated Tests
* Run `.\gradlew compileDebugKotlin --no-daemon` to ensure compilation succeeds.
* Run `.\gradlew testDebugUnitTest --no-daemon` to verify no regressions.

### Manual Verification
* Change language in Settings and check that the UI immediately updates to the chosen language.
* Log out and ensure the profile selection screen resets to the system default language.
