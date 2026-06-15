package com.pixelbuddy.app.presentation.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelbuddy.app.data.audio.AudioPlayer
import com.pixelbuddy.app.data.remote.AIService
import com.pixelbuddy.app.data.repository.ChatRepository
import com.pixelbuddy.app.data.repository.StoryRepository
import com.pixelbuddy.app.domain.model.Story
import com.pixelbuddy.app.domain.model.StoryCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class StoryUiState(
    val stories: List<Story> = emptyList(),
    val selectedCategory: StoryCategory? = null,
    val currentStory: Story? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,           // 0..1
    val currentParagraphIndex: Int = 0,
    val totalParagraphs: Int = 0,
    val statusText: String = "选择故事",
    val isBedtimeMode: Boolean = false,
    val bedtimeMinutes: Int = 15,        // 定时分钟数
    val bedtimeRemaining: Int = 0,       // 剩余秒数
    val volumeLevel: Float = 1f          // 1..0（睡前渐弱）
)

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val aiService: AIService,
    private val audioPlayer: AudioPlayer,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StoryUiState())
    val state: StateFlow<StoryUiState> = _state.asStateFlow()

    private var playbackJob: Job? = null
    private var bedtimeJob: Job? = null

    init {
        loadStories()
    }

    private fun loadStories() {
        _state.value = _state.value.copy(stories = storyRepository.getAllStories())
    }

    fun selectCategory(category: StoryCategory?) {
        val filtered = if (category == null) storyRepository.getAllStories()
        else storyRepository.getByCategory(category)

        _state.value = _state.value.copy(
            selectedCategory = category,
            stories = filtered
        )
    }

    /**
     * 选择一个故事并开始朗读。
     */
    fun playStory(story: Story) {
        stopPlayback()

        val paragraphs = story.text.split("。", "\n").filter { it.isNotBlank() }
        _state.value = _state.value.copy(
            currentStory = story,
            isPlaying = true,
            progress = 0f,
            currentParagraphIndex = 0,
            totalParagraphs = paragraphs.size,
            statusText = "正在朗读……"
        )

        playbackJob = viewModelScope.launch {
            val config = chatRepository.getActiveConfig()

            for ((i, paragraph) in paragraphs.withIndex()) {
                if (!_state.value.isPlaying) break

                val text = paragraph.trim() + "。"
                val ttsResult = aiService.synthesize(config, text)

                if (ttsResult.isFailure) {
                    _state.value = _state.value.copy(
                        statusText = "合成失败: ${ttsResult.exceptionOrNull()?.message}"
                    )
                    delay(1000)
                    continue
                }

                val audioData = ttsResult.getOrThrow()
                var segmentDone = false

                audioPlayer.play(audioData) {
                    segmentDone = true
                }

                // Wait for this segment to finish
                while (!segmentDone && _state.value.isPlaying) {
                    delay(100)
                }

                if (!_state.value.isPlaying) break

                _state.value = _state.value.copy(
                    currentParagraphIndex = i + 1,
                    progress = (i + 1).toFloat() / paragraphs.size
                )
            }

            // Story finished
            if (_state.value.isPlaying) {
                _state.value = _state.value.copy(
                    isPlaying = false,
                    statusText = "故事讲完啦 ✨",
                    progress = 1f
                )
                delay(2000)
                _state.value = _state.value.copy(statusText = "选择故事")
            }
        }
    }

    /**
     * 暂停/继续播放。
     */
    fun togglePause() {
        if (_state.value.isPlaying) {
            audioPlayer.pause()
            playbackJob?.cancel()
            _state.value = _state.value.copy(isPlaying = false, statusText = "已暂停")
        } else {
            // Resume from current paragraph
            val story = _state.value.currentStory ?: return
            val startIdx = _state.value.currentParagraphIndex
            playStoryFrom(story, startIdx)
        }
    }

    private fun playStoryFrom(story: Story, startIndex: Int) {
        stopPlayback()
        val paragraphs = story.text.split("。", "\n").filter { it.isNotBlank() }

        _state.value = _state.value.copy(
            isPlaying = true,
            statusText = "正在朗读……"
        )

        playbackJob = viewModelScope.launch {
            val config = chatRepository.getActiveConfig()

            for (i in startIndex until paragraphs.size) {
                if (!_state.value.isPlaying) break

                val text = paragraphs[i].trim() + "。"
                val ttsResult = aiService.synthesize(config, text)

                if (ttsResult.isFailure) continue

                val audioData = ttsResult.getOrThrow()
                var segmentDone = false
                audioPlayer.play(audioData) { segmentDone = true }

                while (!segmentDone && _state.value.isPlaying) {
                    delay(100)
                }

                _state.value = _state.value.copy(
                    currentParagraphIndex = i + 1,
                    progress = (i + 1).toFloat() / paragraphs.size
                )
            }

            _state.value = _state.value.copy(
                isPlaying = false, statusText = "故事讲完啦 ✨", progress = 1f
            )
        }
    }

    /**
     * 停止播放。
     */
    fun stopPlayback() {
        playbackJob?.cancel()
        audioPlayer.stop()
        _state.value = _state.value.copy(
            isPlaying = false,
            progress = 0f,
            currentParagraphIndex = 0,
            statusText = "选择故事"
        )
    }

    /**
     * 开启睡前模式：定时关闭 + 音量渐弱。
     */
    fun startBedtimeMode(minutes: Int = 15) {
        stopBedtimeMode()

        _state.value = _state.value.copy(
            isBedtimeMode = true,
            bedtimeMinutes = minutes,
            bedtimeRemaining = minutes * 60,
            statusText = "睡前模式 · ${minutes}分钟"
        )

        bedtimeJob = viewModelScope.launch {
            val totalSeconds = minutes * 60
            for (remaining in totalSeconds downTo 0) {
                if (!_state.value.isBedtimeMode) break

                _state.value = _state.value.copy(
                    bedtimeRemaining = remaining,
                    volumeLevel = if (remaining < 60) remaining / 60f else 1f
                )
                delay(1000)
            }

            // 时间到：停止播放
            if (_state.value.isBedtimeMode) {
                stopPlayback()
                _state.value = _state.value.copy(
                    isBedtimeMode = false,
                    statusText = "晚安 🌙"
                )
            }
        }
    }

    fun stopBedtimeMode() {
        bedtimeJob?.cancel()
        _state.value = _state.value.copy(
            isBedtimeMode = false,
            bedtimeRemaining = 0,
            volumeLevel = 1f
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
        stopBedtimeMode()
    }
}
