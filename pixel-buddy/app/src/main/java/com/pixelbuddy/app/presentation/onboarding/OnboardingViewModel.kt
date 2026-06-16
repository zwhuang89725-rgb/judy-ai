package com.pixelbuddy.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelbuddy.app.data.repository.ChatRepository
import com.pixelbuddy.app.domain.model.ModelConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val step: Int = 0,                // 0=欢迎, 1=选预设, 2=填Key, 3=完成
    val selectedPreset: String = "OpenAI",
    val apiKey: String = "",
    val isTesting: Boolean = false,
    val testResult: String? = null,   // null=未测试, "ok"=成功, error msg
    val isSaving: Boolean = false,
    val isDone: Boolean = false       // 触发导航回主页
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun selectPreset(preset: String) {
        _state.update { it.copy(selectedPreset = preset) }
    }

    fun updateApiKey(key: String) {
        _state.update { it.copy(apiKey = key) }
    }

    fun nextStep() {
        _state.update { it.copy(step = it.step + 1) }
    }

    fun prevStep() {
        _state.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }
    }

    fun goToStep(step: Int) {
        _state.update { it.copy(step = step) }
    }

    /**
     * 测试 API 连接（简单发一个空请求验证 key）。
     */
    fun testConnection() {
        val s = _state.value
        if (s.apiKey.isBlank()) {
            _state.update { it.copy(testResult = "请先输入 API Key") }
            return
        }

        _state.update { it.copy(isTesting = true, testResult = null) }

        viewModelScope.launch {
            val presetConfig = when (s.selectedPreset) {
                "OpenAI" -> ModelConfig.OPENAI_PRESET
                "MiMo-TTS" -> ModelConfig.MIMO_TTS_PRESET
                else -> ModelConfig.OPENAI_PRESET
            }

            val testConfig = presetConfig.copy(
                chatApiKey = s.apiKey,
                sttApiKey = s.apiKey,
                ttsApiKey = s.apiKey
            )

            try {
                // 简单测试：发一个最短的 chat 请求
                chatRepository.testConnection(testConfig)
                _state.update { it.copy(isTesting = false, testResult = "ok") }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isTesting = false,
                        testResult = "连接失败：${e.message?.take(60) ?: "未知错误"}"
                    )
                }
            }
        }
    }

    /**
     * 保存配置并标记完成。
     */
    fun saveAndFinish() {
        val s = _state.value
        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val presetConfig = when (s.selectedPreset) {
                "OpenAI" -> ModelConfig.OPENAI_PRESET
                "MiMo-TTS" -> ModelConfig.MIMO_TTS_PRESET
                else -> ModelConfig.OPENAI_PRESET
            }

            val config = presetConfig.copy(
                id = "active",
                name = s.selectedPreset,
                chatApiKey = s.apiKey,
                sttApiKey = s.apiKey,
                ttsApiKey = s.apiKey
            )

            chatRepository.saveConfig(config)

            _state.update { it.copy(isSaving = false, isDone = true) }
        }
    }

    /**
     * 跳过引导（使用默认配置）。
     */
    fun skip() {
        _state.update { it.copy(isDone = true) }
    }
}
