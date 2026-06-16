package com.pixelbuddy.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelbuddy.app.data.repository.ChatRepository
import com.pixelbuddy.app.domain.model.ChatMessage
import com.pixelbuddy.app.domain.model.MessageRole
import com.pixelbuddy.app.domain.usecase.VoiceChatState
import com.pixelbuddy.app.domain.usecase.VoiceChatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val voiceState: VoiceChatState = VoiceChatState(),
    val isSending: Boolean = false,          // 是否正在等待 AI 回复
    val errorMessage: String? = null         // 最近一次错误信息
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val voiceChatUseCase: VoiceChatUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    /** 合并 DB 消息 + 流式消息 */
    val messages: StateFlow<List<ChatMessage>> = combine(
        chatRepository.observeMessages(),
        chatRepository.streamingMessage
    ) { dbMessages, streamingMsg ->
        if (streamingMsg != null) {
            dbMessages + streamingMsg
        } else {
            dbMessages
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val voiceState: StateFlow<VoiceChatState> = voiceChatUseCase.state

    fun onInputChanged(text: String) {
        _inputText.value = text
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** 清除错误提示 */
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 发送文字消息（流式）。
     */
    fun sendTextMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank() || _uiState.value.isSending) return

        _inputText.value = ""
        _uiState.update { it.copy(isSending = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val config = chatRepository.getActiveConfig()
                chatRepository.sendMessageStream(text, config)
                // 等待流式完成（通过 streamingMessage 变为 null 判断）
                chatRepository.streamingMessage
                    .filter { it == null }
                    .first()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "发送失败: ${e.message?.take(60) ?: "请检查网络和 API 配置"}",
                        isSending = false
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(isSending = false) }
        }
    }

    /**
     * 重试上一次发送（需要传最后一条用户消息）。
     */
    fun retryLastMessage() {
        viewModelScope.launch {
            val msgs = chatRepository.observeMessages().first()
            val lastUserMsg = msgs.lastOrNull { it.role == MessageRole.USER }
            if (lastUserMsg != null) {
                _inputText.value = lastUserMsg.content
                sendTextMessage()
            }
        }
    }

    /**
     * 开始录音
     */
    fun startRecording() {
        voiceChatUseCase.startRecording()
        // 启动音量轮询
        viewModelScope.launch {
            while (voiceChatUseCase.state.value.isRecording) {
                voiceChatUseCase.updateAmplitude()
                kotlinx.coroutines.delay(100)
            }
        }
    }

    /**
     * 停止录音并处理
     */
    fun stopRecording() {
        viewModelScope.launch {
            voiceChatUseCase.stopRecordingAndProcess()
        }
    }

    /**
     * 取消录音
     */
    fun cancelRecording() {
        voiceChatUseCase.cancelRecording()
    }

    /**
     * 停止 AI 语音播放
     */
    fun stopPlayback() {
        voiceChatUseCase.stopPlayback()
    }

    /**
     * 清空聊天记录
     */
    fun clearHistory() {
        viewModelScope.launch {
            chatRepository.clearHistory()
            _uiState.update { it.copy(errorMessage = null) }
        }
    }
}
