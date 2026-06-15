package com.pixelbuddy.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelbuddy.app.data.repository.AvatarRepository
import com.pixelbuddy.app.data.repository.ChatRepository
import com.pixelbuddy.app.data.remote.AIService
import com.pixelbuddy.app.domain.model.ModelConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    // Chat
    val chatPreset: String = "DeepSeek",
    val chatBaseUrl: String = "https://api.deepseek.com/v1",
    val chatApiKey: String = "",
    val chatModel: String = "deepseek-chat",
    // STT
    val sttPreset: String = "阿里云 Paraformer",
    val sttBaseUrl: String = "",
    val sttApiKey: String = "",
    val sttModel: String = "paraformer-v2",
    // TTS
    val ttsPreset: String = "MiniMax (MiMo) 免费",
    val ttsBaseUrl: String = "",
    val ttsApiKey: String = "",
    val ttsModel: String = "mimo-v2.5-tts",
    // System
    val systemPrompt: String = ModelConfig.DEFAULT_SYSTEM_PROMPT,
    val isSaving: Boolean = false,
    val saveMessage: String? = null,
    // 模型下拉
    val availableModels: List<String> = emptyList(),
    val isFetchingModels: Boolean = false,
    val modelFetchError: String? = null,
    val showModelDropdown: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val aiService: AIService,
    val avatarRepository: AvatarRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init { loadConfig() }

    private fun loadConfig() {
        viewModelScope.launch {
            val c = chatRepository.getActiveConfig()
            _state.update {
                it.copy(
                    chatBaseUrl = c.chatBaseUrl, chatApiKey = c.chatApiKey, chatModel = c.chatModel,
                    sttBaseUrl = c.sttBaseUrl, sttApiKey = c.sttApiKey, sttModel = c.sttModel,
                    ttsBaseUrl = c.ttsBaseUrl, ttsApiKey = c.ttsApiKey, ttsModel = c.ttsModel,
                    systemPrompt = c.systemPrompt
                )
            }
        }
    }

    // ═══ Chat 预设 ═══
    fun selectChatPreset(preset: ModelConfig.Companion.ChatPreset) {
        _state.update {
            it.copy(
                chatPreset = preset.name,
                chatBaseUrl = preset.baseUrl,
                chatModel = preset.model
            )
        }
    }
    fun updateChatUrl(v: String) { _state.update { it.copy(chatBaseUrl = v) } }
    fun updateChatModel(v: String) { _state.update { it.copy(chatModel = v) } }
    fun updateChatKey(v: String) { _state.update { it.copy(chatApiKey = v) } }
    val isChatCustom: Boolean get() = _state.value.chatPreset.contains("自定义")

    // ═══ STT 预设 ═══
    fun selectSttPreset(preset: ModelConfig.Companion.SttPreset) {
        _state.update {
            it.copy(
                sttPreset = preset.name,
                sttBaseUrl = preset.baseUrl,
                sttModel = preset.model
            )
        }
    }
    fun updateSttKey(v: String) { _state.update { it.copy(sttApiKey = v) } }

    // ═══ TTS 预设 ═══
    fun selectTtsPreset(preset: ModelConfig.Companion.TtsPreset) {
        _state.update {
            it.copy(
                ttsPreset = preset.name,
                ttsBaseUrl = preset.baseUrl,
                ttsModel = preset.model
            )
        }
    }
    fun updateTtsKey(v: String) { _state.update { it.copy(ttsApiKey = v) } }

    fun updateSystemPrompt(v: String) { _state.update { it.copy(systemPrompt = v) } }

    fun toggleModelDropdown() {
        _state.update { it.copy(showModelDropdown = !it.showModelDropdown) }
    }

    fun selectModel(model: String) {
        _state.update { it.copy(chatModel = model, showModelDropdown = false) }
    }

    /**
     * 调 /v1/models 拉取可用模型列表。
     */
    fun fetchModels() {
        val s = _state.value
        if (s.chatBaseUrl.isBlank()) {
            _state.update { it.copy(modelFetchError = "请先填写 Base URL") }
            return
        }
        _state.update { it.copy(isFetchingModels = true, modelFetchError = null) }

        viewModelScope.launch {
            aiService.fetchModels(s.chatBaseUrl, s.chatApiKey)
                .onSuccess { models ->
                    _state.update {
                        it.copy(
                            isFetchingModels = false,
                            availableModels = models,
                            showModelDropdown = true,
                            modelFetchError = if (models.isEmpty()) "该端点未返回模型" else null
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isFetchingModels = false,
                            modelFetchError = "获取失败: ${e.message?.take(40)}"
                        )
                    }
                }
        }
    }

    fun saveConfig() {
        val s = _state.value
        _state.update { it.copy(isSaving = true, saveMessage = null) }

        viewModelScope.launch {
            chatRepository.saveConfig(
                ModelConfig(
                    id = "active", name = "${s.chatPreset} + ${s.ttsPreset}",
                    chatBaseUrl = s.chatBaseUrl, chatApiKey = s.chatApiKey, chatModel = s.chatModel,
                    sttBaseUrl = s.sttBaseUrl, sttApiKey = s.sttApiKey, sttModel = s.sttModel,
                    ttsBaseUrl = s.ttsBaseUrl, ttsApiKey = s.ttsApiKey, ttsModel = s.ttsModel,
                    systemPrompt = s.systemPrompt
                )
            )
            _state.update { it.copy(isSaving = false, saveMessage = "✅ 配置已保存") }
            kotlinx.coroutines.delay(3000)
            _state.update { it.copy(saveMessage = null) }
        }
    }
}
