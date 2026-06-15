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
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val voiceState: VoiceChatState = VoiceChatState()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val voiceChatUseCase: VoiceChatUseCase
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = chatRepository.observeMessages()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val voiceState: StateFlow<VoiceChatState> = voiceChatUseCase.state

    fun onInputChanged(text: String) {
        _inputText.value = text
    }

    /**
     * 发送文字消息
     */
    fun sendTextMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return

        _inputText.value = ""
        viewModelScope.launch {
            chatRepository.sendMessage(text, chatRepository.getActiveConfig())
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
            // 刷新消息列表
            // (sendMessage 在 repository 中已保存)
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
        viewModelScope.launch { chatRepository.clearHistory() }
    }
}
