package com.pixelbuddy.app.data.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 流式音频录制器 — 基于 AudioRecord，持续采集 PCM 帧。
 * 用于「实时语音模式」，不输出文件，通过 Flow 实时推送音频帧。
 *
 * 配置：16kHz 采样率，单声道，16-bit PCM。
 * 每次回调推送 ~100ms 的音频帧（3200 bytes = 1600 samples）。
 */
@Singleton
class StreamingAudioRecorder @Inject constructor() {

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 实时 PCM 帧流（每帧 ~100ms）
    private val _audioFrames = MutableSharedFlow<ShortArray>(
        replay = 0,
        extraBufferCapacity = 8
    )
    val audioFrames: SharedFlow<ShortArray> = _audioFrames

    // 累计音频缓冲区（用于端点截断时发送完整语音段）
    private val accumulatedAudio = ByteArrayOutputStream()

    @Volatile var isRecording: Boolean = false
        private set

    /**
     * 开始持续录音。PCM 帧通过 [audioFrames] Flow 实时推送。
     */
    fun startStreaming() {
        if (isRecording) return

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        ).apply {
            if (state != AudioRecord.STATE_INITIALIZED) {
                release()
                throw IllegalStateException("AudioRecord 初始化失败，请检查麦克风权限")
            }
            startRecording()
        }

        isRecording = true
        accumulatedAudio.reset()

        val shortBuffer = ShortArray(bufferSize / 2)

        recordingJob = scope.launch {
            try {
                while (isActive && isRecording) {
                    val readSize = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: -1
                    if (readSize > 0) {
                        val frame = shortBuffer.copyOf(readSize)
                        _audioFrames.emit(frame)

                        // 写入累计缓冲区（字节）
                        val byteBuffer = ShortArrayToByteArray(frame)
                        accumulatedAudio.write(byteBuffer)
                    }
                }
            } catch (e: Exception) {
                // 录制异常，静默停止
                stopStreaming()
            }
        }
    }

    /**
     * 停止流式录音。
     * @return 本次对话累计的完整 PCM 字节（16-bit little-endian）
     */
    fun stopStreaming(): ByteArray {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.apply {
                try { stop() } catch (_: Exception) {}
                try { release() } catch (_: Exception) {}
            }
        } catch (_: Exception) {
        } finally {
            audioRecord = null
        }

        val data = accumulatedAudio.toByteArray()
        accumulatedAudio.reset()
        return data
    }

    /**
     * 获取当前累计音频（不停止录音），用于端点截断。
     */
    fun getAccumulatedAndReset(): ByteArray {
        val data = accumulatedAudio.toByteArray()
        accumulatedAudio.reset()
        return data
    }

    fun release() {
        stopStreaming()
        scope.cancel()
    }

    /**
     * ShortArray → ByteArray (little-endian 16-bit PCM)
     */
    private fun ShortArrayToByteArray(samples: ShortArray): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val sample = samples[i].toInt()
            bytes[i * 2] = (sample and 0xFF).toByte()
            bytes[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }
        return bytes
    }
}
