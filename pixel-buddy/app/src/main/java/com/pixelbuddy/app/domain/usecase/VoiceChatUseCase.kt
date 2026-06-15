package com.pixelbuddy.app.domain.usecase

import com.pixelbuddy.app.data.audio.AudioPlayer
import com.pixelbuddy.app.data.audio.AudioRecorder
import com.pixelbuddy.app.data.remote.AIService
import com.pixelbuddy.app.domain.model.ChatMessage
import com.pixelbuddy.app.domain.model.MessageRole
import com.pixelbuddy.app.domain.model.ModelConfig
import com.pixelbuddy.app.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音聊天流水线状态
 */
data class VoiceChatState(
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,    // STT + Chat 进行中
    val isPlaying: Boolean = false,       // TTS 播放中
    val transcript: String = "",          // 识别到的文字
    val amplitude: Int = 0,              // 当前录音音量 0..32767
    val statusText: String = "按住说话",
    val error: String? = null
)

/**
 * 语音聊天核心流水线：
 *   按住录音 → 松开 → STT 识别 → AI 对话 → TTS 合成 → 播放
 *
 * 支持中途打断 AI 播放（重新按录音键即停止播放）。
 */
@Singleton
class VoiceChatUseCase @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val aiService: AIService,
    private val chatRepository: ChatRepository
) {
    private val _state = MutableStateFlow(VoiceChatState())
    val state: StateFlow<VoiceChatState> = _state.asStateFlow()

    private var pendingAudioData: ByteArray? = null

    /**
     * 开始录音（按下按钮时调用）
     */
    fun startRecording() {
        // 如果正在播放 AI 语音，打断
        if (audioPlayer.isPlaying) {
            audioPlayer.stop()
            _state.value = _state.value.copy(isPlaying = false)
        }

        audioRecorder.startRecording()
        _state.value = VoiceChatState(
            isRecording = true,
            statusText = "正在听……"
        )
    }

    /**
     * 停止录音并触发完整流水线（松开按钮时调用）
     */
    suspend fun stopRecordingAndProcess() {
        if (!audioRecorder.isRecording) return

        val audioData = audioRecorder.stopRecording()
        if (audioData.isEmpty()) {
            _state.value = VoiceChatState(statusText = "没听清，再试一次？", error = "录音为空")
            return
        }

        _state.value = _state.value.copy(
            isRecording = false,
            isProcessing = true,
            statusText = "识别中……"
        )

        val config = chatRepository.getActiveConfig()

        // Step 1: STT
        val transcribeResult = aiService.transcribe(config, audioData)

        if (transcribeResult.isFailure) {
            _state.value = VoiceChatState(
                statusText = "识别失败，请重试",
                error = transcribeResult.exceptionOrNull()?.message
            )
            return
        }

        val transcript = transcribeResult.getOrThrow()
        _state.value = _state.value.copy(
            transcript = transcript,
            statusText = "思考中……"
        )

        // Save user message
        chatRepository.sendMessage(transcript, config)

        // Step 2: AI Chat (this already saves the response in the repository)
        if (transcript.isBlank()) {
            _state.value = VoiceChatState(statusText = "没听清，再试一次？")
            return
        }

        // Step 3: Get AI response (re-use the repository's sendMessage which handles saving)
        // Actually sendMessage already sends + saves, so we need to get the response differently
        val messages = listOf(
            mapOf("role" to "system", "content" to config.systemPrompt),
            mapOf("role" to "user", "content" to transcript)
        )
        val chatResult = aiService.chat(config, messages)

        if (chatResult.isFailure) {
            _state.value = VoiceChatState(
                statusText = "回复失败，请重试",
                error = chatResult.exceptionOrNull()?.message
            )
            return
        }

        val reply = chatResult.getOrThrow()
        _state.value = _state.value.copy(
            isProcessing = false,
            statusText = "合成语音中……"
        )

        // Step 4: TTS
        val ttsResult = aiService.synthesize(config, reply)

        if (ttsResult.isFailure) {
            // TTS 失败不影响文字显示
            _state.value = VoiceChatState(
                isProcessing = false,
                statusText = "按住说话",
                error = "语音合成失败: ${ttsResult.exceptionOrNull()?.message}"
            )
            return
        }

        val ttsAudio = ttsResult.getOrThrow()
        _state.value = _state.value.copy(
            isPlaying = true,
            statusText = "正在说话……"
        )

        // Step 5: Play TTS audio
        audioPlayer.play(ttsAudio) {
            _state.value = VoiceChatState(statusText = "按住说话")
        }
    }

    /**
     * 取消录音（手指滑出按钮时调用）
     */
    fun cancelRecording() {
        audioRecorder.cancelRecording()
        _state.value = VoiceChatState(statusText = "按住说话")
    }

    /**
     * 停止 AI 语音播放
     */
    fun stopPlayback() {
        audioPlayer.stop()
        _state.value = _state.value.copy(isPlaying = false, statusText = "按住说话")
    }

    /**
     * 更新录音音量（供 UI 定时轮询）
     */
    fun updateAmplitude() {
        if (audioRecorder.isRecording) {
            _state.value = _state.value.copy(amplitude = audioRecorder.getCurrentAmplitude())
        }
    }
}
