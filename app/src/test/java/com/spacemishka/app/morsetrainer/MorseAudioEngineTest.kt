package com.spacemishka.app.morsetrainer

import com.spacemishka.app.morsetrainer.audio.MorseAudioEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MorseAudioEngineTest {

    @Test
    fun testMorseMappings() {
        val map = MorseAudioEngine.MORSE_MAP
        assertEquals(".-", map['A'])
        assertEquals("-...", map['B'])
        assertEquals("-.-.", map['C'])
        assertEquals(".", map['E'])
        assertEquals("...", map['S'])
        assertEquals("---", map['O'])
        
        // Number mappings
        assertEquals(".----", map['1'])
        assertEquals("-----", map['0'])

        // Punctuation mappings
        assertEquals("..--..", map['?'])
        assertEquals("-..-.", map['/'])
        assertEquals("-...-", map['='])
    }

    @Test
    fun testDurationProperties() {
        val engine = MorseAudioEngine()
        engine.speedWpm = 20
        assertEquals(20, engine.speedWpm)
        
        engine.effectiveSpeedWpm = 12
        assertEquals(12, engine.effectiveSpeedWpm)

        engine.toneFrequencyHz = 700
        assertEquals(700, engine.toneFrequencyHz)

        engine.volumePercent = 90
        assertEquals(90, engine.volumePercent)
    }
}
