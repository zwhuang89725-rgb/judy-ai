package com.pixelbuddy.app.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelbuddy.app.data.audio.AudioPlayer
import com.pixelbuddy.app.data.audio.AudioRecorder
import com.pixelbuddy.app.data.remote.AIService
import com.pixelbuddy.app.data.repository.ChatRepository
import com.pixelbuddy.app.domain.model.GameType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameListState(
    val activeSession: GameSession? = null
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val aiService: AIService,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _gameListState = MutableStateFlow(GameListState())
    val gameListState: StateFlow<GameListState> = _gameListState.asStateFlow()

    private val _gamePlayState = MutableStateFlow(GamePlayState())
    val gamePlayState: StateFlow<GamePlayState> = _gamePlayState.asStateFlow()

    private var currentSession: GameSession? = null

    /**
     * 启动一个游戏。
     * 新增游戏时不需要修改此方法 — 只需在 [GameRegistry] 注册。
     */
    fun startGame(type: GameType) {
        // 先停止当前游戏
        stopGame()

        val session = when (type) {
            GameType.ECHO_SPEAK -> EchoSpeakGame(aiService, audioRecorder, audioPlayer, chatRepository)
            GameType.SOUND_GUESS -> SoundGuessGame(aiService, audioPlayer, chatRepository)
            GameType.COLOR_SHAPE -> ColorShapeGame(aiService, audioPlayer, chatRepository)
        } ?: return

        currentSession = session
        session.start()

        _gameListState.value = GameListState(activeSession = session)

        // 收集游戏状态
        viewModelScope.launch {
            session.state.collect { playState ->
                _gamePlayState.value = playState
            }
        }
    }

    fun stopGame() {
        currentSession?.release()
        currentSession = null
        _gameListState.value = GameListState(activeSession = null)
        _gamePlayState.value = GamePlayState()
    }

    /**
     * 用户按住说话按钮，开始录音。
     */
    fun startRecording() {
        audioRecorder.startRecording()
    }

    /**
     * 用户松开按钮 → 停止录音 → STT → 传给游戏处理。
     */
    fun stopRecording() {
        val audioData = audioRecorder.stopRecording()
        if (audioData.isEmpty()) return

        viewModelScope.launch {
            try {
                val config = chatRepository.getActiveConfig()
                val result = aiService.transcribe(config, audioData)
                result.onSuccess { text ->
                    currentSession?.onUserInput(text.trim())
                }
            } catch (_: Exception) {
                // STT 失败，忽略
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopGame()
    }
}
