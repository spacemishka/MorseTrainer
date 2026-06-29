package com.spacemishka.app.morsetrainer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacemishka.app.morsetrainer.audio.MorseAudioEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

// ==========================================
// 1. Morse Hangman ViewModel
// ==========================================
class HangmanViewModel : ViewModel() {
    private val audioEngine = MorseAudioEngine()
    private val wordList = listOf("RADIO", "HAM", "KEY", "TRANSCEIVER", "ANTENNA", "CQ", "SENDER", "RECEIVER", "PADDLE", "CHICKEN", "OSCAR", "DELTA", "GOLF", "MIKE", "ECHO", "WHISKEY", "YANKEE", "XRAY", "VICTOR", "JULIET", "KILO", "LIMA", "NOVEMBER", "SIERRA", "TANGO", "BRAVO", "FOXTROT", "BANANA", "SOS", "MAYDAY", "TEST", "HELLO", "ELEPHANT", "PHONE", "BOBCAT", "RAILWAY", "WATERFALL", "SQUIRREL")

    private val _currentWord = MutableStateFlow("")
    val currentWord = _currentWord.asStateFlow()

    private val _revealedWord = MutableStateFlow("")
    val revealedWord = _revealedWord.asStateFlow()

    private val _remainingAttempts = MutableStateFlow(6)
    val remainingAttempts = _remainingAttempts.asStateFlow()

    private val _clueChar = MutableStateFlow(' ')
    val clueChar = _clueChar.asStateFlow()

    private val _gameStatus = MutableStateFlow("PLAYING") // "PLAYING", "WON", "LOST"
    val gameStatus = _gameStatus.asStateFlow()

    private val _guessedLetters = MutableStateFlow<Set<Char>>(emptySet())
    val guessedLetters = _guessedLetters.asStateFlow()

    init {
        startNewGame()
    }

    fun startNewGame() {
        val word = wordList.random().uppercase()
        _currentWord.value = word
        _revealedWord.value = "_".repeat(word.length)
        _remainingAttempts.value = 6
        _gameStatus.value = "PLAYING"
        _guessedLetters.value = emptySet()
        pickNextClue()
    }

    private fun pickNextClue() {
        val word = _currentWord.value
        val revealed = _revealedWord.value
        
        // Find indices of still hidden letters
        val hiddenIndices = mutableListOf<Int>()
        for (i in word.indices) {
            if (revealed[i] == '_') {
                hiddenIndices.add(i)
            }
        }

        if (hiddenIndices.isNotEmpty()) {
            val randomIdx = hiddenIndices.random()
            _clueChar.value = word[randomIdx]
            playClueSound()
        }
    }

    fun playClueSound() {
        if (_gameStatus.value == "PLAYING" && _clueChar.value != ' ') {
            audioEngine.playText(_clueChar.value.toString())
        }
    }

    fun makeGuess(guessChar: Char) {
        if (_gameStatus.value != "PLAYING") return
        val upperGuess = guessChar.uppercaseChar()
        if (_guessedLetters.value.contains(upperGuess)) return
        val targetClue = _clueChar.value
        val word = _currentWord.value

        if (upperGuess == targetClue) {
            _guessedLetters.value = _guessedLetters.value + upperGuess
            // Correct guess! Reveal ALL instances of this letter in the word
            val revealed = _revealedWord.value.toCharArray()
            for (i in word.indices) {
                if (word[i] == targetClue) {
                    revealed[i] = targetClue
                }
            }
            _revealedWord.value = String(revealed)

            // Check win condition
            if (!_revealedWord.value.contains('_')) {
                _gameStatus.value = "WON"
            } else {
                pickNextClue()
            }
        } else {
            // Wrong guess
            // Only disable the letter if it's not in the word at all
            if (!word.contains(upperGuess)) {
                _guessedLetters.value = _guessedLetters.value + upperGuess
            }
            _remainingAttempts.value = _remainingAttempts.value - 1
            if (_remainingAttempts.value <= 0) {
                _gameStatus.value = "LOST"
                _revealedWord.value = word // Reveal full word
            } else {
                // Play sound again as dynamic reminder
                playClueSound()
            }
        }
    }

    fun stopAudio() {
        audioEngine.stop()
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}

// ==========================================
// 2. Morse Treasure Hunt ViewModel
// ==========================================
class TreasureHuntViewModel : ViewModel() {
    private val audioEngine = MorseAudioEngine()

    data class Clue(val morseCode: String, val plainText: String, val hint: String)

    private val clues = listOf(
        Clue(".... .- .-.. .-.. ---", "HELLO", "A standard greeting in CW"),
        Clue(".- -. - . -. -. .-", "ANTENNA", "Used to propagate electromagnetic waves"),
        Clue(".-. .- -.. .. ---", "RADIO", "Our primary listening medium"),
        Clue("..-. .. -. -..", "FIND", "Search and discover the location"),
        Clue("-.-. --- -.. .", "CODE", "The language of dots and dashes"),
        Clue("-.-. --.-", "CQ", "Calling any station")
    )

    private val _currentClueIndex = MutableStateFlow(0)
    val currentClueIndex = _currentClueIndex.asStateFlow()

    private val _feedbackState = MutableStateFlow("") // "CORRECT", "INCORRECT", ""
    val feedbackState = _feedbackState.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted = _isCompleted.asStateFlow()

    fun getCurrentClue(): Clue? {
        val idx = _currentClueIndex.value
        return if (idx < clues.size) clues[idx] else null
    }

    fun playClueSound() {
        val clue = getCurrentClue() ?: return
        audioEngine.playText(clue.plainText)
    }

    fun submitAnswer(answer: String) {
        val clue = getCurrentClue() ?: return
        if (answer.trim().uppercase() == clue.plainText) {
            _feedbackState.value = "CORRECT"
            viewModelScope.launch {
                delay(1500)
                _feedbackState.value = ""
                val nextIdx = _currentClueIndex.value + 1
                if (nextIdx < clues.size) {
                    _currentClueIndex.value = nextIdx
                    playClueSound()
                } else {
                    _isCompleted.value = true
                }
            }
        } else {
            _feedbackState.value = "INCORRECT"
            viewModelScope.launch {
                delay(2000)
                _feedbackState.value = ""
            }
        }
    }

    fun restartHunt() {
        _currentClueIndex.value = 0
        _feedbackState.value = ""
        _isCompleted.value = false
        playClueSound()
    }

    fun stopAudio() {
        audioEngine.stop()
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}

// ==========================================
// 3. Morse Typing Challenge ViewModel
// ==========================================
class TypingChallengeViewModel : ViewModel() {
    private val audioEngine = MorseAudioEngine()
    private var streamJob: Job? = null

    private val _score = MutableStateFlow(0)
    val score = _score.asStateFlow()

    private val _streak = MutableStateFlow(0)
    val streak = _streak.asStateFlow()

    private val _bestStreak = MutableStateFlow(0)
    val bestStreak = _bestStreak.asStateFlow()

    private val _currentStreamChar = MutableStateFlow("")
    val currentStreamChar = _currentStreamChar.asStateFlow()

    private val _gameState = MutableStateFlow("IDLE") // "IDLE", "RUNNING", "FINISHED"
    val gameState = _gameState.asStateFlow()

    private val _timeRemaining = MutableStateFlow(60) // 60-second games
    val timeRemaining = _timeRemaining.asStateFlow()

    // Character pool of letters + numbers
    private val charPool = ('A'..'Z') + ('0'..'9')

    fun startGame() {
        _score.value = 0
        _streak.value = 0
        _bestStreak.value = 0
        _currentStreamChar.value = ""
        _timeRemaining.value = 60
        _gameState.value = "RUNNING"

        // Set speed higher for typing challenge
        audioEngine.speedWpm = 22
        audioEngine.effectiveSpeedWpm = 18

        startTimer()
        startStream()
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (_timeRemaining.value > 0 && _gameState.value == "RUNNING") {
                delay(1000)
                _timeRemaining.value = _timeRemaining.value - 1
            }
            endGame()
        }
    }

    private fun startStream() {
        streamJob?.cancel()
        audioEngine.stop()
        streamJob = viewModelScope.launch {
            while (_gameState.value == "RUNNING") {
                val nextChar = charPool.random().toString()
                _currentStreamChar.value = nextChar
                audioEngine.playText(nextChar)
                
                // Allow user about 2.0 seconds to type before next character starts
                delay(2200)
            }
        }
    }

    fun submitInput(input: String) {
        if (_gameState.value != "RUNNING") return
        val target = _currentStreamChar.value
        if (target.isEmpty()) return

        if (input.trim().uppercase() == target.uppercase()) {
            val newStreak = _streak.value + 1
            _streak.value = newStreak
            if (newStreak > _bestStreak.value) {
                _bestStreak.value = newStreak
            }
            
            // Score formula: points * streak multiplier
            val multiplier = 1 + (newStreak / 5)
            _score.value = _score.value + (10 * multiplier)
            
            // Instantly skip to next stream character
            startStream()
        } else {
            _streak.value = 0
        }
    }

    fun endGame() {
        _gameState.value = "FINISHED"
        streamJob?.cancel()
        audioEngine.stop()
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}

// ==========================================
// 4. Morse Memory Game ViewModel
// ==========================================
class MemoryViewModel : ViewModel() {
    
    data class MemoryCard(
        val id: Int,
        val text: String,
        val morsePattern: String,
        val type: CardType, // CHAR or MORSE
        val isFaceUp: Boolean = false,
        val isMatched: Boolean = false
    )

    enum class CardType { CHAR, MORSE }

    private val _cards = MutableStateFlow<List<MemoryCard>>(emptyList())
    val cards = _cards.asStateFlow()

    private val _moves = MutableStateFlow(0)
    val moves = _moves.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted = _isCompleted.asStateFlow()

    private var selectedIndex1: Int? = null
    private var selectedIndex2: Int? = null

    init {
        startNewGame()
    }

    fun startNewGame() {
        val pool = listOf(
            "A" to ".-", "E" to ".", "I" to "..", "M" to "--", 
            "N" to "-.", "T" to "-", "O" to "---", "S" to "..."
        )
        
        val newCards = mutableListOf<MemoryCard>()
        var cardId = 0

        for (item in pool) {
            newCards.add(MemoryCard(cardId++, item.first, item.second, CardType.CHAR))
            newCards.add(MemoryCard(cardId++, item.first, item.second, CardType.MORSE))
        }

        newCards.shuffle()
        _cards.value = newCards
        _moves.value = 0
        _isCompleted.value = false
        selectedIndex1 = null
        selectedIndex2 = null
    }

    fun selectCard(index: Int) {
        val currentCards = _cards.value.toMutableList()
        val selectedCard = currentCards[index]

        if (selectedCard.isFaceUp || selectedCard.isMatched) return

        currentCards[index] = selectedCard.copy(isFaceUp = true)
        _cards.value = currentCards

        if (selectedIndex1 == null) {
            selectedIndex1 = index
        } else if (selectedIndex2 == null) {
            selectedIndex2 = index
            _moves.value = _moves.value + 1
            checkMatch()
        }
    }

    private fun checkMatch() {
        val idx1 = selectedIndex1 ?: return
        val idx2 = selectedIndex2 ?: return
        
        viewModelScope.launch {
            delay(1000)
            val currentCards = _cards.value.toMutableList()
            val card1 = currentCards[idx1]
            val card2 = currentCards[idx2]

            // Match condition: same text/morsePattern but different card types
            if (card1.text == card2.text && card1.type != card2.type) {
                currentCards[idx1] = card1.copy(isMatched = true)
                currentCards[idx2] = card2.copy(isMatched = true)
            } else {
                currentCards[idx1] = card1.copy(isFaceUp = false)
                currentCards[idx2] = card2.copy(isFaceUp = false)
            }

            _cards.value = currentCards
            selectedIndex1 = null
            selectedIndex2 = null

            // Check game end
            if (currentCards.all { it.isMatched }) {
                _isCompleted.value = true
            }
        }
    }
}

// ==========================================
// 5. QSO Simulator ViewModel
// ==========================================
class QsoSimulatorViewModel : ViewModel() {
    private val audioEngine = MorseAudioEngine()

    data class QsoStep(
        val systemPrompt: String,      // Audio played from virtual peer
        val hintText: String,          // Decoded text view hint
        val userOptions: List<String>,  // Multiple choice responses
        val correctOptionIdx: Int
    )

    private val steps = listOf(
        QsoStep(
            systemPrompt = "CQ CQ DE DL1ABC K",
            hintText = "Peer calling CQ (calling anyone) from Germany",
            userOptions = listOf("DL1ABC DE IN3XYZ K", "CQ CQ DE IN3XYZ K", "73 DE IN3XYZ K"),
            correctOptionIdx = 0
        ),
        QsoStep(
            systemPrompt = "IN3XYZ DE DL1ABC GA UR RST 599 HR IN BERLIN OP HANS K",
            hintText = "Peer reports signal report 599, Berlin location, name Hans",
            userOptions = listOf("DL1ABC DE IN3XYZ GA UR RST 579 IN ROME OP PIETRO K", "73 HANS DE PIETRO K", "R DE DL1ABC K"),
            correctOptionIdx = 0
        ),
        QsoStep(
            systemPrompt = "IN3XYZ DE DL1ABC R OK DR PIETRO GL VY 73 SK",
            hintText = "Peer says Roger, wishes good luck, 73 (best regards), signing off",
            userOptions = listOf("R DE IN3XYZ SK", "DL1ABC DE IN3XYZ R TU 73 E E", "GA HANS K"),
            correctOptionIdx = 1
        )
    )

    private val _currentStepIdx = MutableStateFlow(0)
    val currentStepIdx = _currentStepIdx.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted = _isCompleted.asStateFlow()

    private val _feedback = MutableStateFlow("") // "CORRECT", "INCORRECT", ""
    val feedback = _feedback.asStateFlow()

    fun startQso() {
        _currentStepIdx.value = 0
        _isCompleted.value = false
        _feedback.value = ""
        playStepSound()
    }

    fun playStepSound() {
        val idx = _currentStepIdx.value
        if (idx < steps.size) {
            audioEngine.playText(steps[idx].systemPrompt)
        }
    }

    fun getCurrentStep(): QsoStep? {
        val idx = _currentStepIdx.value
        return if (idx < steps.size) steps[idx] else null
    }

    fun selectOption(optionIdx: Int) {
        val step = getCurrentStep() ?: return
        if (optionIdx == step.correctOptionIdx) {
            _feedback.value = "CORRECT"
            viewModelScope.launch {
                delay(1500)
                _feedback.value = ""
                val nextIdx = _currentStepIdx.value + 1
                if (nextIdx < steps.size) {
                    _currentStepIdx.value = nextIdx
                    playStepSound()
                } else {
                    _isCompleted.value = true
                }
            }
        } else {
            _feedback.value = "INCORRECT"
        }
    }

    fun stopAudio() {
        audioEngine.stop()
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
