package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognizerHelper(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isCurrentlyListening = false

    interface SpeechListener {
        fun onReady()
        fun onPartialResult(text: String)
        fun onFinalResult(text: String)
        fun onError(errorMsg: String)
    }

    fun startListening(contextToUse: Context, listener: SpeechListener) {
        stopListening()
        
        val contextForAudio = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                contextToUse.createAttributionContext("speech_recognition")
            } catch (e: Exception) {
                contextToUse
            }
        } else {
            contextToUse
        }

        if (!SpeechRecognizer.isRecognitionAvailable(contextForAudio)) {
            listener.onError("お使いの端末は音声認識をサポートしていません。代わりにキーボード入力シミュレートが可能です。")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(contextForAudio).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isCurrentlyListening = true
                        listener.onReady()
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    
                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "マイク音声入力エラー"
                            SpeechRecognizer.ERROR_CLIENT -> "端末エラー"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "マイクの権限がありません"
                            SpeechRecognizer.ERROR_NETWORK -> "通信エラー"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "通信タイムアウト"
                            SpeechRecognizer.ERROR_NO_MATCH -> "音声を認識できません"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "音声認識システムがビジー状態です"
                            SpeechRecognizer.ERROR_SERVER -> "サーバーエラー"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "音声タイムアウト"
                            else -> "音声認識エラー"
                        }
                        Log.e("SpeechRecognizerHelper", "OnError: $message ($error)")
                        listener.onError("$message ($error)")
                        isCurrentlyListening = false
                    }

                    override fun onResults(results: Bundle?) {
                        isCurrentlyListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            listener.onFinalResult(text)
                        } else {
                            listener.onFinalResult("")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            listener.onPartialResult(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                startListening(intent)
            }
        } catch (e: Exception) {
            listener.onError("音声認識初期化エラー: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.apply {
                stopListening()
                cancel()
                destroy()
            }
        } catch (e: Exception) {
            Log.e("SpeechRecognizerHelper", "Error stopping recognizer", e)
        }
        speechRecognizer = null
        isCurrentlyListening = false
    }

    fun isListening(): Boolean = isCurrentlyListening
}
