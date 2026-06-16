package com.pixelbuddy.app.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 音频录制器 — 基于 MediaRecorder，输出 WAV 文件。
 * 用于「按住说话」模式，每次录制生成一个临时文件。
 */
@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean get() = recorder != null

    /**
     * 开始录制。返回输出文件的路径。
     */
    fun startRecording(): File {
        stopRecording() // 确保没有残留

        outputFile = File(context.cacheDir, "voice_input_${System.currentTimeMillis()}.wav")

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16000)
            setAudioEncodingBitRate(64000)
            setOutputFile(outputFile!!.absolutePath)
            prepare()
            start()
        }

        return outputFile!!
    }

    /**
     * 停止录制。返回录制的音频文件字节数组，失败返回空。
     */
    fun stopRecording(): ByteArray {
        val file = outputFile
        try {
            recorder?.apply {
                try { stop() } catch (_: Exception) {}
                try { release() } catch (_: Exception) {}
            }
        } catch (_: Exception) {
        } finally {
            recorder = null
            outputFile = null
        }

        return if (file != null && file.exists() && file.length() > 0) {
            file.readBytes()
        } else {
            ByteArray(0)
        }
    }

    /**
     * 取消录制（丢弃文件）。
     */
    fun cancelRecording() {
        try {
            recorder?.apply {
                try { stop() } catch (_: Exception) {}
                try { release() } catch (_: Exception) {}
            }
        } catch (_: Exception) {
        } finally {
            recorder = null
        }
        outputFile?.delete()
        outputFile = null
    }

    /**
     * 获取当前录制时长（毫秒）。仅用于 UI 展示。
     */
    fun getCurrentAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (_: Exception) {
            0
        }
    }
}
