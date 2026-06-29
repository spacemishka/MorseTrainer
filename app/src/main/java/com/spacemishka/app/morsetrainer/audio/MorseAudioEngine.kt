package com.spacemishka.app.morsetrainer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MorseAudioEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        const val ENVELOPE_RAMP_MS = 5

        val MORSE_MAP = mapOf(
            'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
            'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
            'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
            'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
            'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
            'Z' to "--..",
            '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-", '5' to ".....",
            '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.", '0' to "-----",
            '.' to ".-.-.-", '?' to "..--..", '/' to "-..-.", '=' to "-...-"
        )
    }

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val engineJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + engineJob)

    // Configuration parameters
    var toneFrequencyHz: Int = 600
    var volumePercent: Int = 80 // 0 to 100
    var speedWpm: Int = 20
    var effectiveSpeedWpm: Int = 10
    var audioProfile: String = "SINE" // "SINE", "NOISY", "FADING"

    // Dynamic fading variable
    private var fadingPhase = 0.0

    @Synchronized
    private fun initAudioTrack() {
        if (audioTrack == null) {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = if (minBufferSize > 0) minBufferSize else 4096

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }
        
        if (audioTrack?.state == AudioTrack.STATE_INITIALIZED && audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack?.play()
        }
    }

    @Synchronized
    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                flush()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
        fadingPhase = 0.0
    }

    fun release() {
        stop()
        engineJob.cancel()
    }

    fun playText(text: String, onFinished: () -> Unit = {}) {
        stop()
        playbackJob = scope.launch {
            try {
                initAudioTrack()
                
                // Calculate durations based on WPM and Farnsworth
                val tDotMs = (1200.0 / speedWpm).toInt()
                val tDashMs = tDotMs * 3
                val tElementSpaceMs = tDotMs
                
                // Calculate character space and word space with Farnsworth stretching
                val tCharSpaceMs: Int
                val tWordSpaceMs: Int

                if (effectiveSpeedWpm < speedWpm) {
                    // Standard word "PARIS" is 50 units total:
                    // 31 units inside characters (22 sound + 9 intra-symbol space)
                    // 19 units total spacing (4 character spaces @ 3 units = 12 units, 1 word space @ 7 units = 7 units)
                    val totalWordTimeMs = (60000.0 / effectiveSpeedWpm)
                    val soundTimeMs = 31 * tDotMs
                    val spaceTimeMs = totalWordTimeMs - soundTimeMs

                    if (spaceTimeMs > 0) {
                        // Divide remaining space by ratio 3:7 (12 units for char gaps, 7 units for word gaps)
                        // A character gap consists of 3 units
                        // A word gap consists of 7 units
                        val unitSpaceMs = spaceTimeMs / 19.0
                        tCharSpaceMs = (3 * unitSpaceMs).toInt()
                        tWordSpaceMs = (7 * unitSpaceMs).toInt()
                    } else {
                        tCharSpaceMs = tDotMs * 3
                        tWordSpaceMs = tDotMs * 7
                    }
                } else {
                    tCharSpaceMs = tDotMs * 3
                    tWordSpaceMs = tDotMs * 7
                }

                val uppercaseText = text.uppercase()
                
                for (i in uppercaseText.indices) {
                    if (!isActive) break
                    val char = uppercaseText[i]

                    if (char == ' ') {
                        // Word space gap. Wait since word space gap includes character spaces around it.
                        // Standard word gap is 7 units. Since we already added character gap after the previous letter,
                        // we add (tWordSpaceMs - tCharSpaceMs) to complete the word gap.
                        val remainingWordGap = (tWordSpaceMs - tCharSpaceMs).coerceAtLeast(0)
                        playSilence(remainingWordGap)
                    } else {
                        val morsePattern = MORSE_MAP[char]
                        if (morsePattern != null) {
                            for (j in morsePattern.indices) {
                                if (!isActive) break
                                val symbol = morsePattern[j]
                                val symbolDuration = if (symbol == '.') tDotMs else tDashMs
                                
                                playTone(symbolDuration)
                                
                                if (j < morsePattern.length - 1) {
                                    playSilence(tElementSpaceMs)
                                }
                            }
                            
                            // Character gap
                            // Wait between characters unless it's the last character and a space follows
                            val nextIsSpace = (i < uppercaseText.length - 1 && uppercaseText[i + 1] == ' ')
                            if (i < uppercaseText.length - 1 && !nextIsSpace) {
                                playSilence(tCharSpaceMs)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    onFinished()
                }
            }
        }
    }

    private suspend fun playTone(durationMs: Int) {
        val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val rampSamples = (SAMPLE_RATE * (ENVELOPE_RAMP_MS / 1000.0)).toInt().coerceAtMost(totalSamples / 2)
        val buffer = ShortArray(totalSamples)

        val baseVolume = (volumePercent / 100.0) * Short.MAX_VALUE

        for (i in 0 until totalSamples) {
            if (!coroutineContext.isActive) return

            // Calculate basic sine wave value
            val angle = 2.0 * PI * toneFrequencyHz * i / SAMPLE_RATE
            var wave = sin(angle)

            // Dynamic Fading (QSB)
            if (audioProfile == "FADING") {
                // Modulate amplitude slowly over time (e.g. 0.15 Hz cycle)
                val fadingFactor = 0.7 + 0.3 * sin(fadingPhase)
                wave *= fadingFactor
                fadingPhase += 2.0 * PI * 0.15 / SAMPLE_RATE
                if (fadingPhase > 2.0 * PI) {
                    fadingPhase -= 2.0 * PI
                }
            }

            // Background Noise (QRM)
            if (audioProfile == "NOISY") {
                // Add low-level white noise (approx 5% volume)
                val noise = Random.nextDouble(-0.05, 0.05)
                wave += noise
            }

            // Apply raised-cosine envelope to prevent key clicks
            val envelope = when {
                i < rampSamples -> {
                    0.5 * (1.0 - cos(PI * i / rampSamples))
                }
                i > totalSamples - rampSamples -> {
                    val remaining = totalSamples - i
                    0.5 * (1.0 - cos(PI * remaining / rampSamples))
                }
                else -> 1.0
            }

            buffer[i] = (wave * envelope * baseVolume).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        writeToTrack(buffer)
    }

    private suspend fun playSilence(durationMs: Int) {
        if (durationMs <= 0) return
        val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)

        // If noisy, play low-level background noise even during silence
        if (audioProfile == "NOISY") {
            val baseVolume = (volumePercent / 100.0) * Short.MAX_VALUE
            for (i in 0 until totalSamples) {
                if (!coroutineContext.isActive) return
                val noise = Random.nextDouble(-0.03, 0.03) // Silent noise is slightly quieter
                buffer[i] = (noise * baseVolume).toInt().toShort()
            }
        }

        writeToTrack(buffer)
    }

    private suspend fun writeToTrack(buffer: ShortArray) = withContext(Dispatchers.IO) {
        var written = 0
        while (written < buffer.size && coroutineContext.isActive) {
            val track = audioTrack ?: break
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                try {
                    track.play()
                } catch (e: Exception) {
                    break
                }
            }
            val result = track.write(buffer, written, buffer.size - written)
            if (result > 0) {
                written += result
            } else if (result == 0) {
                // Avoid spin-waiting in case of buffer issues
                delay(10)
            } else {
                // Negative error code - break to prevent infinite spin-waiting
                break
            }
        }
    }
}
