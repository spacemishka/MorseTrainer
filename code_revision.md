# MorseTrainer — Code Revision Report

**Date:** 2026-06-29  
**Scope:** Full project review — all Kotlin source files  
**Total files analysed:** 14

---

## Summary

| Severity | Count |
|----------|-------|
| 🔴 Critical (crash / data-loss risk) | 4 |
| 🟠 High (logic bug / wrong behaviour) | 7 |
| 🟡 Medium (UX regression / performance) | 8 |
| 🔵 Low (style / robustness) | 7 |
| **Total** | **26** |

---

## 🔴 Critical Issues

### C-1 · `UserProfileDao` — Non-suspend DAO methods called on main thread
**File:** [`UserProfileDao.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/data/dao/UserProfileDao.kt)  
**Lines:** 18, 21, 24, 27

```kotlin
// Current — blocking, non-suspend Room queries
fun getProfileById(id: Long): UserProfile?
fun insertProfile(profile: UserProfile): Long
fun updateProfile(profile: UserProfile)
fun deleteProfile(profile: UserProfile)
```

**Problem:** Room requires DAO methods that run on the database to be either `suspend` (coroutine) or return `LiveData` / `Flow`. Non-suspend, non-returning methods that hit the database will throw an **`IllegalStateException`** ("Cannot access database on the main thread") unless the caller manually dispatches to `Dispatchers.IO`. The current callers in `MorseViewModel` do wrap in `withContext(Dispatchers.IO)`, but if anyone adds a call without that boilerplate the app will crash. It is also wrong to have blocking calls inside a Room DAO in modern Android development.

**Fix:** Add the `suspend` modifier to all mutation methods:
```kotlin
@Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
suspend fun getProfileById(id: Long): UserProfile?

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertProfile(profile: UserProfile): Long

@Update
suspend fun updateProfile(profile: UserProfile)

@Delete
suspend fun deleteProfile(profile: UserProfile)
```
Apply the same fix to [`UserStatisticDao.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/data/dao/UserStatisticDao.kt) and [`AchievementDao.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/data/dao/AchievementDao.kt) for the same reason.

---

### C-2 · `MorseAudioEngine` — `CoroutineScope` never cancelled → resource leak
**File:** [`MorseAudioEngine.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/audio/MorseAudioEngine.kt)  
**Line:** 40

```kotlin
private val scope = CoroutineScope(Dispatchers.Default) // never cancelled
```

**Problem:** The engine's private `CoroutineScope` is never cancelled. If the engine is stopped (`stop()` is called) the playback `Job` is cancelled, but the scope itself lives forever. Successive calls to `playText()` keep spawning new child coroutines in this permanent scope. Each game ViewModel owns its *own* `MorseAudioEngine` instance (5 engines total), meaning 5 perpetual scopes exist in the process lifetime.

**Fix:** Use `SupervisorJob` + a cancel-method, or replace the scope with the caller's `viewModelScope` passed in as a parameter. The cleanest fix for a reusable engine is:
```kotlin
private val engineJob = SupervisorJob()
private val scope = CoroutineScope(Dispatchers.Default + engineJob)

fun stop() {
    playbackJob?.cancel()
    playbackJob = null
    engineJob.cancel() // cancel all children
    engineJob.children.forEach { it.join() } // optional drain
    // re-create job for next playText() call if needed
    ...
}
```

---

### C-3 · `MemoryViewModel` — Mutable `data class` fields mutated directly
**File:** [`GameViewModels.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/GameViewModels.kt)  
**Lines:** 347, 371–375

```kotlin
selectedCard.isFaceUp = true   // mutates the list item directly
_cards.value = currentCards    // then re-emits the same list reference
```

**Problem:** `MemoryCard` is a `data class` with `var` properties. The code mutates the object that is already inside the `MutableStateFlow`. Compose may not recompose because the reference it holds hasn't changed (it's the same object). This is a classic state bug that can cause the card flip animation/display to silently not update.

**Fix:** Use immutable `val` in `MemoryCard` and always replace via `copy()`:
```kotlin
data class MemoryCard(
    val id: Int,
    val text: String,
    val morsePattern: String,
    val type: CardType,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

// When flipping:
val updatedCard = selectedCard.copy(isFaceUp = true)
currentCards[index] = updatedCard
_cards.value = currentCards
```

---

### C-4 · `MainActivity` — `applyLocale` uses deprecated `updateConfiguration`
**File:** [`MainActivity.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/MainActivity.kt)  
**Lines:** 94, 99

```kotlin
resources.updateConfiguration(configuration, resources.displayMetrics) // deprecated API 25
```

**Problem:** `Resources.updateConfiguration()` was deprecated in API 25 and ignored from API 35 onward. On Android 15 devices this silently does nothing, breaking the language override feature completely. It also mutates a `Configuration` object that is shared, which can have unexpected side-effects.

**Fix:** Use `LocaleListCompat` and `AppCompatDelegate` (the AndroidX-recommended approach):
```kotlin
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

private fun applyLocale(languageCode: String) {
    val localeList = if (languageCode == "SYSTEM") {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(languageCode)
    }
    AppCompatDelegate.setApplicationLocales(localeList)
    // No need to call recreate() — AppCompat handles it automatically
}
```

---

## 🟠 High Issues

### H-1 · `KochProgressionEngine.getActiveCharacters` — off-by-one level logic duplicated differently in the UI
**Files:** [`KochProgressionEngine.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/domain/KochProgressionEngine.kt) L19, [`MorseAppScreens.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/screens/MorseAppScreens.kt) L496

Engine:
```kotlin
val end = (level + 1).coerceIn(2, sequence.length) // level=1 → end=2
```
UI:
```kotlin
val count = (it.currentKochLevel + 1).coerceIn(2, it.customSequence.length)
```

**Problem:** The formula is copy-pasted in the UI composable instead of calling `kochEngine.getActiveCharacters()`. If the engine logic ever changes there will be a divergence. Additionally, `StatsScreen` uses yet another formula (`profile?.currentKochLevel?.let { it + 1 } ?: 2` at line 1024) for `activeCount`, making three separate calculations.

**Fix:** Expose `getActiveCharacters()` from the ViewModel and use a single source of truth everywhere. Create an `activeCharacters: StateFlow<List<String>>` derived from `_currentProfile` in `MorseViewModel`.

---

### H-2 · `TypingChallengeViewModel` — `startStream()` called from `submitInput()` creates unbounded Job growth
**File:** [`GameViewModels.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/GameViewModels.kt)  
**Lines:** 269, 238–250

```kotlin
fun submitInput(input: String) {
    ...
    startStream()   // <-- creates a new Job
}
private fun startStream() {
    streamJob?.cancel()
    streamJob = viewModelScope.launch { ... }
}
```

**Problem:** Every correct answer calls `startStream()`, which cancels the old job and creates a brand-new one. However, the `delay(2200)` in the old stream job only suspends; it doesn't respond to cancellation before the delay completes. Rapid correct answers may cause multiple jobs to stack up before cancellation takes effect. More critically, the audio engine `playText()` for the *old* character continues to play through because `audioEngine.stop()` is not called before launching the new stream.

**Fix:** Call `audioEngine.stop()` in `startStream()` before creating the new coroutine:
```kotlin
private fun startStream() {
    streamJob?.cancel()
    audioEngine.stop()  // stop any ongoing audio immediately
    streamJob = viewModelScope.launch { ... }
}
```

---

### H-3 · `MorseViewModel.handleUserInput` — promotion check uses session list from UI, not from DB
**File:** [`MorseViewModel.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/MorseViewModel.kt)  
**Lines:** 217–231

```kotlin
val attempts = _recentAttempts.value.toMutableList()
attempts.add(isCorrect)
_recentAttempts.value = attempts
...
if (profile != null && kochEngine.shouldPromote(attempts, profile.kochWindowSize)) {
    _unlockedPromotion.value = true
}
```

**Problem:** `_recentAttempts` is reset to empty on `resetSessionStats()` and `selectProfile()`. If the user navigates away and comes back mid-session the list resets to zero, and the promotion threshold can be re-triggered with only `windowSize` attempts in the *new* session, even if the user had previously failed. The persistent per-character statistics in the DB are never consulted for the promotion decision — only the volatile in-memory list.

**Fix:** Either persist `recentAttempts` in the database or derive the promotion check from the actual DB statistics (overall `lastAccuracy` per character). At minimum, save the session list in `onSaveInstanceState` via `SavedStateHandle`.

---

### H-4 · `BackupService.importBackup` — no duplication guard; importing twice creates ghost profiles
**File:** [`BackupService.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/data/BackupService.kt)  
**Lines:** 52–68

**Problem:** Every import unconditionally inserts new profiles. Calling import twice (or importing a backup that contains profiles already present by name) will create exact duplicates, with different IDs. There is no name-uniqueness constraint on `user_profiles`.

**Fix:** Add a unique constraint on `user_profiles.name` (or check by name before inserting), or present a conflict dialog to the user. Alternatively, provide an "overwrite" vs "merge" option.

---

### H-5 · `HangmanViewModel` — guessed letters not tracked; keyboard letters stay active forever
**File:** [`GameViewModels.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/GameViewModels.kt)  
**Line:** 215 (comment in screen file)

```kotlin
val isUsed = false // Simplified. Can be extended to grey out guessed letters if needed
```

**Problem:** The `isUsed` flag is hardcoded `false`. Users can press the same wrong letter repeatedly, costing themselves lives without any visual feedback that the letter was already tried. This is a significant UX and game logic bug.

**Fix:** Add a `_guessedLetters: MutableStateFlow<Set<Char>>` in `HangmanViewModel`, update it on `makeGuess()`, expose it, and use it in the UI to disable/grey out already-used keys.

---

### H-6 · `TreasureHuntScreen` — answer field not cleared after incorrect answer, leading to stale input
**File:** [`MorseGameScreens.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/screens/MorseGameScreens.kt)  
**Lines:** 337–345

```kotlin
Button(onClick = {
    viewModel.submitAnswer(userAns)
    userAns = ""            // cleared even on wrong answer immediately
})
```

Actually the field IS cleared on submit regardless of outcome. But the `feedbackState` stays `"INCORRECT"` until the user submits again, and there is no way to dismiss it or re-hear the clue sound automatically. Cross-referencing `TreasureHuntViewModel.submitAnswer()`: the feedback is never automatically reset to `""` on `INCORRECT`, only after a correct answer. So the user is stuck at `"INCORRECT"` with no timer to reset.

**Fix:** Auto-reset `feedbackState` to `""` after a short delay on incorrect answer:
```kotlin
"INCORRECT" -> {
    _feedbackState.value = "INCORRECT"
    viewModelScope.launch {
        delay(2000)
        _feedbackState.value = ""
    }
}
```

---

### H-7 · `QsoSimulatorViewModel` — `startQso()` not called on ViewModel init; screen starts blank
**File:** [`GameViewModels.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/GameViewModels.kt)  
**Lines:** 393–470

**Problem:** The `QsoSimulatorViewModel` has no `init { }` block and `startQso()` must be called manually from the UI. In `QsoSimulatorScreen`, `startQso()` is never called automatically — the screen starts with `stepIdx = 0`, `isCompleted = false`, and the audio is never played. The user has to know to press the play button to begin.

**Fix:** Add an `init { startQso() }` block or call `LaunchedEffect(Unit) { viewModel.startQso() }` in the composable so audio plays when entering the screen for the first time.

---

## 🟡 Medium Issues

### M-1 · `MorseAudioEngine.initAudioTrack` — AudioTrack reuse across calls can cause `PLAYSTATE_STOPPED` stall
**File:** [`MorseAudioEngine.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/audio/MorseAudioEngine.kt)  
**Lines:** 53–84

After `stop()` is called, `audioTrack` is set to `null`. But `initAudioTrack()` only creates a new instance when `audioTrack == null`, then calls `play()`. In `writeToTrack`, if `track.playState != PLAYSTATE_PLAYING` it tries `track.play()` again. This can collide with `@Synchronized` on `initAudioTrack` vs the non-synchronized `writeToTrack`. A race condition between `stop()` and `writeToTrack` can result in operating on a released track.

**Fix:** Hold the lock for the full duration of write-then-check, or use a dedicated state machine. At minimum, null-check `audioTrack` inside `writeToTrack` *after* acquiring a reference (the current code does this with `val track = audioTrack ?: break`, which is correct — but the `@Synchronized` on `initAudioTrack` and `stop()` is not applied to `writeToTrack`, making it possible for `stop()` to release `audioTrack` between the null check and the `track.write()` call).

---

### M-2 · `TypingChallengeScreen` — The radar display always shows `"?"` instead of the current character
**File:** [`MorseGameScreens.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/screens/MorseGameScreens.kt)  
**Line:** 438

```kotlin
Text("?", color = ThemeAmber, fontSize = 72.sp, fontWeight = FontWeight.Bold)
```

**Problem:** The character being played is stored in `currentStreamChar` and collected as `currentSymbol` (line 364), but the radar `Box` always shows a hard-coded `"?"`. Players have no visual confirmation of which character was played, which defeats the purpose of the copy-practice game.

**Fix:**
```kotlin
Text(
    text = if (currentSymbol.isEmpty()) "?" else currentSymbol,
    color = ThemeAmber,
    fontSize = 72.sp,
    fontWeight = FontWeight.Bold
)
```
(Or intentionally hide it and reveal only after the user answers — but that intent should be explicit and documented.)

---

### M-3 · `MorseViewModel.createProfile` — `selectProfile(null)` guard missing; race condition possible
**File:** [`MorseViewModel.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/MorseViewModel.kt)  
**Lines:** 112–117

```kotlin
val inserted = withContext(Dispatchers.IO) {
    db.userProfileDao().getProfileById(newId)
}
if (_currentProfile.value == null) {
    selectProfile(inserted)
}
```

**Problem:** If `getProfileById` returns `null` (e.g., due to a DB error) and `_currentProfile.value == null`, then `selectProfile(null)` is called. `selectProfile(null)` removes the stored ID preference but does not guard against calling `generateNextTarget()` with a null profile — `generateNextTarget()` early-returns on null profile, but the SharedPreferences `active_profile_id` has already been cleared, causing unexpected state.

**Fix:** Guard the null case explicitly:
```kotlin
if (inserted != null && _currentProfile.value == null) {
    selectProfile(inserted)
}
```

---

### M-4 · `MainActivity` — Infinite `recreate()` loop risk
**File:** [`MainActivity.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/MainActivity.kt)  
**Lines:** 49–66

```kotlin
LaunchedEffect(profile) {
    ...
    if (targetLang != appliedLanguage) {
        ...
        recreate()
    }
}
```

**Problem:** `appliedLanguage` is set once in `onCreate()` and then only updated inside the `if` block before `recreate()`. But `recreate()` destroys and recreates the `Activity`, so the `ViewModel` (`currentProfile`) may emit the *same profile* again on the new activity, re-triggering the `LaunchedEffect`. If the locale application via `updateConfiguration` fails silently (see C-4), the language stays the same but `appliedLanguage` is updated anyway, so the re-trigger won't loop. However, if `applyLocale()` succeeds, the activity correctly re-creates once. If `C-4` is fixed with `AppCompatDelegate`, recreate is handled automatically and should be removed here.

**Fix:** After fixing C-4, remove the manual `recreate()` call entirely — `AppCompatDelegate.setApplicationLocales()` triggers recreation automatically.

---

### M-5 · `KochProgressionEngine.selectNextCharacter` — weighted selection can return `activeCharacters.last()` as fallback even when it's the `lastTarget`
**File:** [`KochProgressionEngine.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/domain/KochProgressionEngine.kt)  
**Line:** 118

```kotlin
return activeCharacters.last() // Fallback to last character
```

**Problem:** If the last character in `activeCharacters` happens to be the same as `lastTarget`, this fallback will return a consecutive repeat, which the weighting system explicitly tries to prevent.

**Fix:**
```kotlin
val fallback = activeCharacters.lastOrNull { it != lastTarget } ?: activeCharacters.last()
return fallback
```

---

### M-6 · `DashboardScreen` — Backup export shows a hardcoded English `Toast`
**File:** [`MorseAppScreens.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/screens/MorseAppScreens.kt)  
**Lines:** 382, 400, 402, 406

```kotlin
Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
```

**Problem:** Backup-related toast messages are hardcoded English strings, bypassing the localization system. All other UI strings use `stringResource(R.string.*)`.

**Fix:** Add the missing string resources and use `stringResource` or pass the `Context` a resource ID.

---

### M-7 · `SettingsScreen` — Slider `effectiveWpm` can visually exceed `wpm` before `coerceAtMost` fires
**File:** [`MorseAppScreens.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/screens/MorseAppScreens.kt)  
**Lines:** 1151–1168

```kotlin
onValueChange = {
    wpm = it
    if (effectiveWpm > wpm) { effectiveWpm = wpm }
}
...
onValueChange = { effectiveWpm = it.coerceAtMost(wpm) },
```

**Problem:** The WPM slider correctly caps `effectiveWpm` when WPM decreases. However, the `effectiveWpm` slider's `valueRange` is `5f..40f` — it doesn't dynamically cap its maximum to `wpm`. A user can drag `effectiveWpm` above `wpm` on the slider (visually), and the value is only capped at the point of emission. This creates a confusing UI state.

**Fix:** Bind the `effectiveWpm` slider's `valueRange` dynamically:
```kotlin
Slider(
    value = effectiveWpm,
    onValueChange = { effectiveWpm = it.coerceAtMost(wpm) },
    valueRange = 5f..wpm,  // dynamic max
    ...
)
```

---

### M-8 · `MorseViewModel.deleteProfile` — `allProfiles` Flow could show deleted profile briefly
**File:** [`MorseViewModel.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/MorseViewModel.kt)  
**Lines:** 154–163

**Problem:** After deleting the current profile, `_currentProfile.value = null` is set inside a `launch` coroutine — on the `Main` dispatcher since it runs after the suspend `withContext(IO)` block returns. This is fine. However, if the user deletes a *non-selected* profile quickly, the `allProfiles` flow may temporarily display the deleted profile until Room's Flow emits the updated list. This is inherent to Room's asynchronous emissions and acceptable, but it means the `deleteProfile` button in `ProfileScreen` is not guarded against double-taps (the profile stays clickable until the list refreshes).

**Fix:** Disable the delete button during the deletion operation by tracking a `Set<Long>` of in-progress deletions.

---

## 🔵 Low / Style Issues

### L-1 · `MorseDatabase` — `fallbackToDestructiveMigration()` will silently delete user data on schema change
**File:** [`MorseDatabase.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/data/database/MorseDatabase.kt)  
**Line:** 36

**Problem:** The database is at version 4, meaning it has already been migrated at least 3 times. Using `fallbackToDestructiveMigration()` means any future version bump without a proper `Migration` object will silently wipe all user profiles and statistics. Acceptable in development, but dangerous in production.

**Recommendation:** Add explicit `Migration` objects for each version step and remove `fallbackToDestructiveMigration()` before shipping to users. At minimum, add a comment warning.

---

### L-2 · `MorseAudioEngine.MORSE_MAP` — Missing common punctuation characters
**File:** [`MorseAudioEngine.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/audio/MorseAudioEngine.kt)  
**Lines:** 25–35

**Problem:** The map is missing several ITU-R standard Morse characters: `,` (`--..--`), `-` (`-....-`), `'` (`.----.`), `(` (`-.--.-`), `)` (`-.--.-`), `:` (`---...`), `;` (`-.-.-.`). The `FreeTextScreen` allows arbitrary text input; entering unsupported characters silently skips them, which may confuse users.

**Recommendation:** Either add missing characters or display a warning when unsupported characters are found in the input text.

---

### L-3 · `KochProgressionEngine.selectNextCharacter` — `sessionTargets` 40% rule checked before lastTarget anti-repeat
**File:** [`KochProgressionEngine.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/domain/KochProgressionEngine.kt)  
**Lines:** 47–61, 64

**Problem:** When the 40% rule forces selection of `newChar`, the function returns it immediately without checking if it's the same as `lastTarget`. This means the new character can be selected consecutively, which the anti-repeat logic (line 71–74) is intended to prevent.

**Fix:**
```kotlin
if (percentage < 0.40 && newChar != lastTarget) {
    return newChar
}
```

---

### L-4 · `MemoryViewModel` — Card pool is always the same 8 characters; no randomization across games
**File:** [`GameViewModels.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/GameViewModels.kt)  
**Lines:** 320–323

**Problem:** `startNewGame()` always uses the same hardcoded 8-character pool (`A, E, I, M, N, T, O, S`). After playing a few times the player has memorized the grid positions. Randomizing or rotating the pool would extend replay value.

**Recommendation:** Maintain a larger character pool derived from `MorseAudioEngine.MORSE_MAP` and pick a random 8 each new game.

---

### L-5 · `ProfileScreen` — No confirmation dialog before deleting a profile
**File:** [`MorseAppScreens.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/screens/MorseAppScreens.kt)  
**Line:** 205

```kotlin
IconButton(onClick = { viewModel.deleteProfile(profile) }) {
    Icon(Icons.Default.Delete, ...)
}
```

**Problem:** A single tap on the delete button immediately and irreversibly deletes the profile and all its statistics/achievements (via CASCADE). The backup restore is the only recovery path, and users may not know about it.

**Recommendation:** Show a confirmation `AlertDialog` before deletion.

---

### L-6 · `MorseViewModel` — `BackupService` instantiated on every export/import call
**File:** [`MorseViewModel.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/MorseViewModel.kt)  
**Lines:** 326, 330

```kotlin
suspend fun exportDataJson(): String {
    return BackupService(db).exportBackup()
}
```

**Problem:** `BackupService` is a lightweight class, so this is not a crash, but it instantiates a new `Json { prettyPrint = true; ignoreUnknownKeys = true }` object on every call (inside `BackupService`). `kotlinx.serialization.json.Json` construction is not free.

**Fix:** Make `BackupService` a singleton or a member of `MorseViewModel`:
```kotlin
private val backupService = BackupService(db)
```

---

### L-7 · `KochLessonScreen` — Both `LaunchedEffect` blocks can fire simultaneously on entry
**File:** [`MorseAppScreens.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/main/java/com/spacemishka/app/morsetrainer/ui/screens/MorseAppScreens.kt)  
**Lines:** 533–543

```kotlin
LaunchedEffect(showIntro, currentIntroChar) {
    if (showIntro) { currentIntroChar?.let { viewModel.playTextDirectly(it) } }
}

LaunchedEffect(lastFeedback, targetChar, showIntro) {
    if (!showIntro && lastFeedback == null && targetChar.isNotEmpty()) {
        viewModel.playCurrentTarget()
    }
}
```

**Problem:** On initial composition, both `LaunchedEffect` blocks execute. If `showIntro` is false initially and a new target is available, `playCurrentTarget()` fires. If the state rapidly flips (e.g., during intro sequence dismissal), both blocks might fire nearly simultaneously, causing overlapping or duplicated audio.

**Recommendation:** Consolidate into a single `LaunchedEffect` with clear priority logic, or add a small guard delay.

---

## Test Coverage Notes

The single test file [`KochProgressionEngineTest.kt`](file:///c:/Users/peter/AndroidStudioProjects/MorseTrainer/app/src/test/java/com/spacemishka/app/morsetrainer/KochProgressionEngineTest.kt) covers `KochProgressionEngine` well. 

**Missing test scenarios:**

| Scenario | Suggested Test |
|---|---|
| `BackupService` round-trip | Export → clear DB → import → verify same data |
| `MorseAudioEngine` Farnsworth timing math | Unit-test `tCharSpaceMs` / `tWordSpaceMs` formulae |
| `MorseViewModel.handleUserInput` promotion edge cases | 89% accuracy should NOT promote |
| `MemoryViewModel.selectCard` prevents 3rd simultaneous flip | Call `selectCard()` 3 times fast |
| `KochProgressionEngine.getActiveCharacters` with empty sequence | Should not crash |

---

## Priority Action Plan

| Priority | Issue | Effort |
|---|---|---|
| 1 | **C-1** — Make all DAO methods `suspend` | Low |
| 2 | **C-3** — Fix `MemoryCard` mutable state bug | Low |
| 3 | **C-4** — Fix locale API via `AppCompatDelegate` | Medium |
| 4 | **H-5** — Track guessed letters in Hangman | Low |
| 5 | **M-2** — Show actual character in Typing Challenge radar | Trivial |
| 6 | **H-6** — Auto-reset INCORRECT feedback in Treasure Hunt | Trivial |
| 7 | **H-7** — Auto-start QSO Simulator on screen entry | Trivial |
| 8 | **C-2** — Fix audio engine scope leak | Medium |
| 9 | **H-2** — Fix typing challenge stream job accumulation | Low |
| 10 | **L-5** — Add delete confirmation dialog | Low |
