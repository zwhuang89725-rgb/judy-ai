package com.pixelbuddy.app.data.audio

import kotlin.math.sqrt

/**
 * 语音活动检测器 (VAD) — 基于能量阈值。
 *
 * 工作原理：
 *   1. 计算每帧 RMS（均方根能量）
 *   2. RMS > 阈值 → 标记为"语音帧"
 *   3. 连续 N 帧语音 → 开始说话
 *   4. 连续 M 帧静音 → 结束说话（端点检测）
 *
 * 配置为适合近距离麦克风的参数。
 */
class VoiceActivityDetector(
    private val energyThreshold: Float = 300f,    // RMS 阈值（需根据设备校准）
    private val speechStartFrames: Int = 5,        // 连续语音帧数 → 开始说话
    private val silenceEndFrames: Int = 20,         // 连续静音帧数 → 结束说话（~2 秒 @100ms/帧）
    private val minSpeechDurationFrames: Int = 10  // 最短有效语音（~1 秒），避免误触发
) {
    enum class State {
        SILENCE,        // 静默
        SPEECH_START,   // 检测到语音开始（过渡态）
        SPEAKING,       // 正在说话
        SPEECH_END      // 语音结束（可截断）
    }

    var state: State = State.SILENCE
        private set

    private var speechFrameCount = 0
    private var silenceFrameCount = 0
    private var currentRms = 0f

    /**
     * 处理一帧 PCM 数据。返回当前 VAD 状态。
     * @param frame PCM 16-bit samples (ShortArray)
     * @return 更新后的 VAD 状态
     */
    fun processFrame(frame: ShortArray): State {
        currentRms = calculateRms(frame)

        when (state) {
            State.SILENCE -> {
                if (currentRms > energyThreshold) {
                    speechFrameCount++
                    if (speechFrameCount >= speechStartFrames) {
                        state = State.SPEECH_START
                        speechFrameCount = 0
                    }
                } else {
                    speechFrameCount = 0
                }
            }

            State.SPEECH_START -> {
                // 过渡态：开始计时语音长度
                speechFrameCount++
                silenceFrameCount = 0
                if (speechFrameCount >= minSpeechDurationFrames) {
                    state = State.SPEAKING
                } else if (currentRms <= energyThreshold) {
                    // 太短了，退回静默
                    state = State.SILENCE
                    speechFrameCount = 0
                }
            }

            State.SPEAKING -> {
                if (currentRms > energyThreshold) {
                    speechFrameCount++
                    silenceFrameCount = 0
                } else {
                    silenceFrameCount++
                    if (silenceFrameCount >= silenceEndFrames) {
                        state = State.SPEECH_END
                    }
                }
            }

            State.SPEECH_END -> {
                // 外部处理完后重置
            }
        }

        return state
    }

    /**
     * 重置 VAD 状态到 SILENCE（用于新一轮对话）。
     */
    fun reset() {
        state = State.SILENCE
        speechFrameCount = 0
        silenceFrameCount = 0
        currentRms = 0f
    }

    /**
     * 当前帧的 RMS 能量值（0..∞，用于 UI 可视化）。
     */
    fun getCurrentEnergy(): Float = currentRms

    /**
     * 归一化能量值 (0..1)，用于 UI 显示。
     */
    fun getNormalizedEnergy(): Float = (currentRms / 2000f).coerceIn(0f, 1f)

    /**
     * 计算 ShortArray 的 RMS（均方根）。
     */
    private fun calculateRms(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0.0
        for (sample in samples) {
            val normalized = sample.toDouble() / 32768.0
            sum += normalized * normalized
        }
        return (sqrt(sum / samples.size) * 32768.0).toFloat()
    }
}
