package com.pixelbuddy.app.domain.usecase

import com.pixelbuddy.app.data.audio.AudioPlayer
import com.pixelbuddy.app.data.audio.StreamingAudioRecorder
import com.pixelbuddy.app.data.audio.VoiceActivityDetector
import com.pixelbuddy.app.data.remote.AIService
import com.pixelbuddy.app.data.repository.ChatRepository
import com.pixelbuddy.app.domain.model.PixelExpression
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 实时语音模式状态
 */
data class AlwaysOnState(
    val isActive: Boolean = false,          // 实时模式是否开启
    val vadState: VoiceActivityDetector.State = VoiceActivityDetector.State.SILENCE,
    val energyLevel: Float = 0f,            // 0..1 音量等级
    val isProcessing: Boolean = false,      // STT + Chat 进行中
    val isPlaying: Boolean = false,         // TTS 播放中
    val currentTranscript: String = "",     // 当前识别文字
    val expression: PixelExpression = PixelExpression.HAPPY,
    val statusText: String = "实时语音已就绪",
    val error: String? = null
)

/**
 * Always-On 实时语音模式 UseCase。
 *
 * 工作流程（像打电话一样）：
 *   1. 启动后持续监听麦克风
 *   2. VAD 检测到用户开始说话 → 记录语音段
 *   3. VAD 检测到用户停止说话（~2s 静音） → 自动截断
 *   4. 发送截断语音到 STT → AI Chat → TTS → 播放回复
 *   5. 播放期间如果 VAD 再次检测到用户说话 → 立即打断，开始新一轮
 *   6. 循环往复，直到用户关闭实时模式
 */
@Singleton
class AlwaysOnVoiceUseCase @Inject constructor(
    private val streamingRecorder: StreamingAudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val vad: VoiceActivityDetector,
    private val aiService: AIService,
    private val chatRepository: ChatRepository
) {
    private val _state = MutableStateFlow(AlwaysOnState())
    val state: StateFlow<AlwaysOnState> = _state.asStateFlow()

    private var monitorJob: Job? = null
    private var processJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 启动实时语音模式。
     */
    fun start() {
        if (_state.value.isActive) return

        vad.reset()
        streamingRecorder.startStreaming()
        _state.value = AlwaysOnState(
            isActive = true,
            expression = PixelExpression.HAPPY,
            statusText = "我在听……直接说话吧"
        )

        // 启动 VAD 监控协程
        monitorJob = scope.launch {
            streamingRecorder.audioFrames.collect { frame ->
                if (!_state.value.isActive) return@collect

                val vadResult = vad.processFrame(frame)
                val energy = vad.getNormalizedEnergy()

                // 更新能量显示
                _state.value = _state.value.copy(
                    vadState = vadResult,
                    energyLevel = energy
                )

                when (vadResult) {
                    VoiceActivityDetector.State.SPEECH_START -> {
                        // 用户开始说话 → 打断 AI 播放
                        interruptPlayback()
                        _state.value = _state.value.copy(
                            expression = PixelExpression.THINKING,
                            statusText = "正在听……"
                        )
                    }

                    VoiceActivityDetector.State.SPEAKING -> {
                        _state.value = _state.value.copy(
                            expression = PixelExpression.TALKING,
                            statusText = "……"
                        )
                    }

                    VoiceActivityDetector.State.SPEECH_END -> {
                        // 用户说完 → 截断音频并处理
                        _state.value = _state.value.copy(
                            expression = PixelExpression.THINKING,
                            statusText = "识别中……",
                            isProcessing = true
                        )

                        val audioData = streamingRecorder.getAccumulatedAndReset()
                        if (audioData.isNotEmpty()) {
                            processSpeechChunk(audioData)
                        }

                        vad.reset()
                        _state.value = _state.value.copy(isProcessing = false)
                    }

                    else -> { /* SILENCE */ }
                }
            }
        }
    }

    /**
     * 停止实时语音模式。
     */
    fun stop() {
        _state.value = AlwaysOnState(
            isActive = false,
            statusText = "实时语音已关闭"
        )
        monitorJob?.cancel()
        processJob?.cancel()
        streamingRecorder.stopStreaming()
        audioPlayer.stop()
        vad.reset()
    }

    /**
     * 处理一个语音段：STT → Chat → TTS → 播放。
     */
    private suspend fun processSpeechChunk(audioData: ByteArray) {
        processJob?.cancel()
        processJob = scope.launch {
            try {
                val config = chatRepository.getActiveConfig()

                // Step 1: STT
                val transcriptResult = aiService.transcribe(config, audioData)
                if (transcriptResult.isFailure) {
                    _state.value = _state.value.copy(
                        statusText = "没听清……",
                        expression = PixelExpression.SURPRISED,
                        error = transcriptResult.exceptionOrNull()?.message
                    )
                    delay(1500)
                    _state.value = _state.value.copy(statusText = "我在听……", error = null)
                    return@launch
                }

                val transcript = transcriptResult.getOrThrow().trim()
                if (transcript.isBlank()) {
                    _state.value = _state.value.copy(statusText = "我在听……")
                    return@launch
                }

                _state.value = _state.value.copy(
                    currentTranscript = transcript,
                    statusText = "思考中……",
                    expression = PixelExpression.THINKING
                )

                // Step 2: Save user msg + get AI reply
                val messages = listOf(
                    mapOf("role" to "system", "content" to config.systemPrompt),
                    mapOf("role" to "user", "content" to transcript)
                )
                val chatResult = aiService.chat(config, messages)

                if (chatResult.isFailure) {
                    _state.value = _state.value.copy(
                        statusText = "网络出问题了……",
                        expression = PixelExpression.SURPRISED,
                        error = chatResult.exceptionOrNull()?.message
                    )
                    delay(1500)
                    _state.value = _state.value.copy(statusText = "我在听……", error = null)
                    return@launch
                }

                val reply = chatResult.getOrThrow()

                // Step 3: TTS
                _state.value = _state.value.copy(statusText = "合成语音……")
                val ttsResult = aiService.synthesize(config, reply)

                if (ttsResult.isFailure) {
                    _state.value = _state.value.copy(
                        statusText = "语音合成失败",
                        expression = PixelExpression.SILLY,
                        error = ttsResult.exceptionOrNull()?.message
                    )
                    delay(1500)
                    _state.value = _state.value.copy(statusText = "我在听……", error = null)
                    return@launch
                }

                val ttsAudio = ttsResult.getOrThrow()

                // Step 4: Play TTS
                _state.value = _state.value.copy(
                    isPlaying = true,
                    expression = PixelExpression.LOVING,
                    statusText = "正在说话……"
                )

                // 使用 suspendCoroutine 等待播放完成或被中断
                var playbackDone = false
                audioPlayer.play(ttsAudio) {
                    playbackDone = true
                }

                // 等待播放完成（或被打断）
                while (!playbackDone && _state.value.isActive && audioPlayer.isPlaying) {
                    delay(100)
                }

                _state.value = _state.value.copy(
                    isPlaying = false,
                    expression = PixelExpression.HAPPY,
                    statusText = "我在听……"
                )

            } catch (e: CancellationException) {
                // 被打断，正常
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    statusText = "出错了…再试一次？",
                    expression = PixelExpression.SURPRISED,
                    error = e.message
                )
                delay(2000)
                _state.value = _state.value.copy(statusText = "我在听……", error = null)
            }
        }
    }

    /**
     * 打断 AI 语音播放。
     */
    private fun interruptPlayback() {
        if (audioPlayer.isPlaying) {
            audioPlayer.stop()
            processJob?.cancel()
            _state.value = _state.value.copy(
                isPlaying = false,
                isProcessing = false,
                statusText = "正在听……"
            )
        }
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
