package com.example.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

class SynthesizedSoundPlayer {
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startPlaying(soundType: String, volume: Float) {
        stopPlaying()
        playbackJob = scope.launch {
            val sampleRate = 22050
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = minBufferSize.coerceAtLeast(sampleRate * 2)

            @Suppress("DEPRECATION")
            val track = AudioTrack(
                AudioManager.STREAM_ALARM,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
            audioTrack = track
            track.setVolume(volume)
            
            try {
                track.play()
            } catch (e: Exception) {
                Log.e("SynthesizedSoundPlayer", "Failed to start AudioTrack play", e)
                return@launch
            }

            val buffer = ShortArray(1024)
            var phase = 0.0

            try {
                while (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    when (soundType) {
                        "CHIME_SYNTH" -> {
                            val elapsedMillis = System.currentTimeMillis()
                            val cycle = (elapsedMillis / 400) % 4
                            val targetFreq = when (cycle) {
                                0L -> 440.0 // A4
                                1L -> 554.37 // C#5
                                2L -> 659.25 // E5
                                else -> 880.0 // A5
                            }
                            for (i in buffer.indices) {
                                val t = phase / sampleRate
                                val wave = sin(2.0 * Math.PI * targetFreq * t) + 0.3 * sin(4.0 * Math.PI * targetFreq * t)
                                buffer[i] = (wave * 12000 * (1.0 - (phase % (sampleRate * 0.4)) / (sampleRate * 0.4))).toInt().toShort()
                                phase += 1.0
                            }
                        }
                        "BEEP_PULSE" -> {
                            val elapsedMillis = System.currentTimeMillis()
                            val isBeeping = (elapsedMillis % 1000) < 400
                            val pulseIdx = if ((elapsedMillis % 1000) < 200) 0 else 1
                            val targetFreq = if (pulseIdx == 0) 880.0 else 1046.5
                            
                            for (i in buffer.indices) {
                                if (isBeeping) {
                                    val t = phase / sampleRate
                                    val wave = sin(2.0 * Math.PI * targetFreq * t)
                                    buffer[i] = (wave * 15000).toInt().toShort()
                                } else {
                                    buffer[i] = 0
                                }
                                phase += 1.0
                            }
                        }
                        "ZEN_MEDITATION" -> {
                            val elapsedMillis = System.currentTimeMillis()
                            val lfo = sin(2.0 * Math.PI * 0.05 * (elapsedMillis / 1000.0))
                            val targetFreq = 220.0 + lfo * 20.0
                            for (i in buffer.indices) {
                                val t = phase / sampleRate
                                val wave = sin(2.0 * Math.PI * targetFreq * t) + 0.4 * sin(2.0 * Math.PI * (targetFreq * 1.5) * t)
                                buffer[i] = (wave * 10000).toInt().toShort()
                                phase += 1.0
                            }
                        }
                        else -> {
                            for (i in buffer.indices) {
                                val t = phase / sampleRate
                                val wave = sin(2.0 * Math.PI * 440.0 * t)
                                buffer[i] = (wave * 15000).toInt().toShort()
                                phase += 1.0
                            }
                        }
                    }
                    track.write(buffer, 0, buffer.size)
                    delay(5)
                }
            } catch (e: Exception) {
                Log.d("SynthesizedSoundPlayer", "Playback loop interrupted or cancelled.")
            }
        }
    }

    fun setPlayingVolume(volume: Float) {
        try {
            audioTrack?.setVolume(volume)
        } catch (e: Exception) {
            Log.e("SynthesizedSoundPlayer", "Failed to set volume on AudioTrack", e)
        }
    }

    fun playOneShotEffect(effectType: String, volume: Float) {
        scope.launch {
            val sampleRate = 22050
            val durationSamples = when (effectType) {
                "EFFECT_START" -> (sampleRate * 0.2).toInt() // 200 ms
                "EFFECT_SUCCESS" -> (sampleRate * 0.6).toInt() // 600 ms
                "EFFECT_FAILURE" -> (sampleRate * 0.4).toInt() // 400 ms
                else -> (sampleRate * 0.3).toInt()
            }
            
            val buffer = ShortArray(durationSamples)
            
            when (effectType) {
                "EFFECT_START" -> {
                    // Two quick sweet notes: E5 (659.25 Hz) then B5 (987.77 Hz)
                    val half = durationSamples / 2
                    for (i in 0 until half) {
                        val t = i.toDouble() / sampleRate
                        val wave = sin(2.0 * Math.PI * 659.25 * t)
                        val env = 1.0 - (i.toDouble() / half)
                        buffer[i] = (wave * 12000 * env).toInt().toShort()
                    }
                    for (i in half until durationSamples) {
                        val t = (i - half).toDouble() / sampleRate
                        val wave = sin(2.0 * Math.PI * 987.77 * t)
                        val env = 1.0 - ((i - half).toDouble() / half)
                        buffer[i] = (wave * 12000 * env).toInt().toShort()
                    }
                }
                "EFFECT_SUCCESS" -> {
                    // do-mi-sol-do / C5 (523.25), E4 (659.25), G5 (783.99), C6 (1046.50)
                    val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
                    val noteSamples = durationSamples / notes.size
                    for (n in notes.indices) {
                        val freq = notes[n]
                        val startIdx = n * noteSamples
                        for (i in 0 until noteSamples) {
                            val idx = startIdx + i
                            if (idx >= buffer.size) break
                            val t = i.toDouble() / sampleRate
                            val wave = sin(2.0 * Math.PI * freq * t) + 0.3 * sin(2.0 * Math.PI * (freq * 2.0) * t)
                            val env = 1.0 - (i.toDouble() / noteSamples)
                            buffer[idx] = (wave * 12000 * env).toInt().toShort()
                        }
                    }
                }
                "EFFECT_FAILURE" -> {
                    // Double low buzz: 180Hz then 140Hz
                    val half = durationSamples / 2
                    val silenceSamples = (sampleRate * 0.04).toInt()
                    for (i in 0 until half - silenceSamples) {
                        val t = i.toDouble() / sampleRate
                        val rawEnv = sin(2.0 * Math.PI * 180.0 * t)
                        val wave = if (rawEnv > 0) 1.0 else -1.0
                        val env = 1.0 - (i.toDouble() / (half - silenceSamples))
                        buffer[i] = (wave * 7000 * env).toInt().toShort()
                    }
                    for (i in half until durationSamples - silenceSamples) {
                        val t = (i - half).toDouble() / sampleRate
                        val rawEnv = sin(2.0 * Math.PI * 140.0 * t)
                        val wave = if (rawEnv > 0) 1.0 else -1.0
                        val env = 1.0 - ((i - half).toDouble() / (half - silenceSamples))
                        buffer[i] = (wave * 7000 * env).toInt().toShort()
                    }
                }
            }
            
            var track: AudioTrack? = null
            try {
                @Suppress("DEPRECATION")
                track = AudioTrack(
                    AudioManager.STREAM_ALARM,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    durationSamples * 2,
                    AudioTrack.MODE_STATIC
                )
                track.write(buffer, 0, buffer.size)
                track.setVolume(volume)
                track.play()
                val playMillis = (durationSamples * 1000L) / sampleRate
                delay(playMillis + 100)
            } catch (e: Exception) {
                Log.e("SynthesizedSoundPlayer", "Error playing one-shot effect", e)
            } finally {
                try {
                    track?.apply {
                        if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                            stop()
                        }
                        release()
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    fun stopPlaying() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.d("SynthesizedSoundPlayer", "Failed to stop AudioTrack gracefully.")
        }
        audioTrack = null
    }
}
