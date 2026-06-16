package com.pixelbuddy.app.presentation.game

import com.pixelbuddy.app.data.audio.AudioPlayer
import com.pixelbuddy.app.data.audio.AudioRecorder
import com.pixelbuddy.app.data.remote.AIService
import com.pixelbuddy.app.data.repository.ChatRepository
import com.pixelbuddy.app.domain.model.Game
import com.pixelbuddy.app.domain.model.GameDifficulty
import com.pixelbuddy.app.domain.model.GameType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 跟读模仿游戏 — 完整语音引导版。
 *
 * AI 全程用语音引导：开场说明规则 → 逐轮说词 → 鼓励/安慰反馈 → 结束总结。
 */
class EchoSpeakGame(
    private val aiService: AIService,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val chatRepository: ChatRepository
) : GameSession {

    override val game = Game(
        id = GameType.ECHO_SPEAK.name,
        type = GameType.ECHO_SPEAK,
        title = "跟读模仿",
        description = "AI 说一个词，你来跟读！比谁说得像～",
        iconEmoji = "🎤",
        difficulty = GameDifficulty.EASY
    )

    private val _state = MutableStateFlow(GamePlayState(totalRounds = 3, statusText = "准备开始"))
    override val state: StateFlow<GamePlayState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val words = listOf(
        "苹果", "太阳", "月亮", "星星", "大象",
        "猴子", "花花", "汽车", "飞机", "老虎",
        "熊猫", "大海", "彩虹", "小鸟", "西瓜"
    )

    private var currentWord = ""

    // ═══════════ 语音播放辅助 ═══════════

    /** 说一句话（异步，等说完才返回） */
    private suspend fun speak(text: String) {
        try {
            val config = chatRepository.getActiveConfig()
            val audio = aiService.synthesize(config, text)
            audio.onSuccess { data ->
                var done = false
                audioPlayer.play(data) { done = true }
                while (!done && _state.value.isRunning) { delay(100) }
            }
        } catch (_: Exception) { /* TTS 失败不影响游戏 */ }
    }

    override fun start() {
        _state.update { it.copy(isRunning = true, isFinished = false, round = 1, score = 0) }

        // 🎤 语音开场引导
        scope.launch {
            _state.update { it.copy(statusText = "游戏开始！") }
            speak("欢迎来到跟读模仿游戏！我说一个词，你跟着我学一遍。准备好了吗？一共三轮，开始喽！")
            nextRound()
        }
    }

    override fun stop() {
        scope.coroutineContext.cancelChildren()
        audioPlayer.stop()
        _state.update { it.copy(isRunning = false, statusText = "游戏结束") }
    }

    override fun onUserInput(text: String) {
        if (!_state.value.isRunning) return

        val s = _state.value
        val isCorrect = text.contains(currentWord) || currentWord.contains(text)

        val newScore = if (isCorrect) s.score + 10 else s.score
        val feedbackText: String
        val voiceFeedback: String

        if (isCorrect) {
            val encouragements = listOf("太棒了！", "说得真好！", "完美！", "你真厉害！")
            feedbackText = encouragements.random() + " ✨"
            voiceFeedback = encouragements.random()
        } else {
            feedbackText = "没关系，再试一次！你说的是「$text」，我说的是「$currentWord」"
            voiceFeedback = "没关系～我说的是「$currentWord」，你说的是「$text」，很接近了！"
        }

        _state.update {
            it.copy(
                childResponse = text,
                score = newScore,
                feedback = feedbackText,
                statusText = if (isCorrect) "答对了！" else "再试一次～"
            )
        }

        scope.launch {
            // 🎤 语音反馈
            speak(voiceFeedback)
            delay(500)

            if (s.round >= s.totalRounds) {
                finish()
            } else {
                _state.update { it.copy(round = it.round + 1) }
                // 🎤 过渡引导
                speak("下一轮！")
                nextRound()
            }
        }
    }

    override fun nextRound() {
        val s = _state.value
        if (s.round > s.totalRounds) { finish(); return }

        currentWord = words.random()

        _state.update {
            it.copy(
                prompt = "「$currentWord」",
                feedback = "",
                childResponse = "",
                statusText = "第 ${s.round}/${s.totalRounds} 轮 · 跟我读"
            )
        }

        scope.launch {
            // 🎤 出题语音（第二轮起加过渡）
            if (s.round > 1) delay(300)
            speak("请跟我读：$currentWord")
            _state.update { it.copy(statusText = "轮到你了！按住说话 🎤") }
        }
    }

    private fun finish() {
        val s = _state.value
        val stars = when {
            s.score >= 25 -> "三颗星"
            s.score >= 15 -> "两颗星"
            else -> "一颗星"
        }

        _state.update {
            it.copy(
                isFinished = true, isRunning = false,
                statusText = "游戏结束！",
                feedback = "得分：${s.score}  ⭐⭐⭐\n你太棒了！再来一局？"
            )
        }

        // 🎤 结束语音总结
        scope.launch {
            speak("游戏结束啦！你一共得到了${s.score}分，获得$stars！你真是太厉害了，给你鼓掌！还想再玩一次吗？")
        }
    }

    override fun release() {
        stop()
        scope.cancel()
    }
}
