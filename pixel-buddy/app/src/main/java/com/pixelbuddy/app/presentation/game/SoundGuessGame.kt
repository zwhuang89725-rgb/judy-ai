package com.pixelbuddy.app.presentation.game

import com.pixelbuddy.app.data.audio.AudioPlayer
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
 * 声音猜谜游戏 — 完整语音引导版。
 *
 * AI 全程用语音引导：开场说明 → 逐轮念谜语 → 鼓励/安慰/公布答案 → 结束总结。
 */
class SoundGuessGame(
    private val aiService: AIService,
    private val audioPlayer: AudioPlayer,
    private val chatRepository: ChatRepository
) : GameSession {

    override val game = Game(
        id = GameType.SOUND_GUESS.name,
        type = GameType.SOUND_GUESS,
        title = "声音猜谜",
        description = "听听这是什么声音？猜对得分！",
        iconEmoji = "🔊",
        difficulty = GameDifficulty.EASY
    )

    private val _state = MutableStateFlow(GamePlayState(totalRounds = 3, statusText = "准备开始"))
    override val state: StateFlow<GamePlayState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val riddles = listOf(
        Riddle("汪汪汪！我有四条腿，喜欢吃骨头，我是人类最好的朋友。我是谁？", listOf("狗", "小狗", "狗狗", "汪汪")),
        Riddle("喵喵喵！我走路静悄悄，喜欢捉老鼠，白天爱晒太阳。我是谁？", listOf("猫", "小猫", "猫咪", "喵喵")),
        Riddle("哞哞哞！我身体很大，身上有黑白花纹，能产牛奶。我是谁？", listOf("牛", "奶牛", "母牛", "哞哞")),
        Riddle("嘎嘎嘎！我有扁扁的嘴巴，脚上有蹼，喜欢在水里游。我是谁？", listOf("鸭", "鸭子", "小鸭", "嘎嘎")),
        Riddle("叽叽叽！我有一对翅膀，清晨会叫人起床，头顶有红冠。我是谁？", listOf("鸡", "公鸡", "小鸡", "母鸡")),
        Riddle("咩咩咩！我身上有卷卷的毛，软软的像云朵。我是谁？", listOf("羊", "绵羊", "小羊", "咩咩")),
        Riddle("嗡嗡嗡！我身体很小，会采花蜜，能做出甜甜的东西。我是谁？", listOf("蜜蜂", "小蜜蜂", "蜂")),
        Riddle("呱呱呱！我小时候是小蝌蚪，长大了会跳，喜欢在池塘边。我是谁？", listOf("青蛙", "小青蛙", "蛙")),
        Riddle("咚咚咚！我有长长的鼻子，大大的耳朵，是陆地上最大的动物。我是谁？", listOf("大象", "象")),
        Riddle("嗷呜！我是森林之王，头上有鬃毛，跑得很快。我是谁？", listOf("狮子", "大狮子", "狮")),
        Riddle("我有八条腿，会吐丝结网，喜欢在角落织家。我是谁？", listOf("蜘蛛", "小蜘蛛", "蛛")),
        Riddle("我有两扇大翅膀，身上五颜六色，从毛毛虫变成的。我是谁？", listOf("蝴蝶", "小蝴蝶", "蝶")),
        Riddle("我住在南极，穿着黑白色的礼服，走路摇摇摆摆。我是谁？", listOf("企鹅", "小企鹅")),
        Riddle("我有硬硬的壳，遇到危险就缩进去，走路慢吞吞。我是谁？", listOf("乌龟", "小乌龟", "龟")),
        Riddle("我有一对长耳朵，红红的眼睛，最喜欢吃胡萝卜。我是谁？", listOf("兔子", "小兔子", "小白兔", "兔")),
    )

    private var currentRiddle: Riddle? = null

    // ═══════════ 语音播放辅助 ═══════════

    private suspend fun speak(text: String) {
        try {
            val config = chatRepository.getActiveConfig()
            val audio = aiService.synthesize(config, text)
            audio.onSuccess { data ->
                var done = false
                audioPlayer.play(data) { done = true }
                while (!done && _state.value.isRunning) { delay(100) }
            }
        } catch (_: Exception) { }
    }

    override fun start() {
        _state.update { it.copy(isRunning = true, isFinished = false, round = 1, score = 0) }

        // 🔊 语音开场
        scope.launch {
            _state.update { it.copy(statusText = "游戏开始！") }
            speak("欢迎来到声音猜谜！我会学一种动物的叫声，然后告诉你它的特点。你来猜猜它是谁。一共三轮，准备好了吗？开始！")
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
        val riddle = currentRiddle ?: return

        val isCorrect = riddle.answers.any { answer -> text.contains(answer) }
        val newScore = if (isCorrect) s.score + 10 else s.score

        val feedbackText: String
        val voiceFeedback: String

        if (isCorrect) {
            val encouragements = listOf(
                "答对啦！你真聪明！",
                "没错，就是${riddle.answers.first()}！好厉害！",
                "完全正确！击掌！"
            )
            feedbackText = encouragements.random() + " ✨"
            voiceFeedback = encouragements.random()
        } else {
            feedbackText = "答案是「${riddle.answers.first()}」～下次记住哦！"
            voiceFeedback = "没关系～答案是「${riddle.answers.first()}」。$text 也很接近了，很棒！"
        }

        _state.update {
            it.copy(
                childResponse = text,
                score = newScore,
                feedback = feedbackText,
                statusText = if (isCorrect) "🎉 答对了！" else "再试试～"
            )
        }

        scope.launch {
            speak(voiceFeedback)
            delay(600)

            if (s.round >= s.totalRounds) {
                finish()
            } else {
                _state.update { it.copy(round = it.round + 1) }
                speak("下一题！")
                nextRound()
            }
        }
    }

    override fun nextRound() {
        val s = _state.value
        if (s.round > s.totalRounds) { finish(); return }

        val riddle = riddles.random()
        currentRiddle = riddle

        _state.update {
            it.copy(
                prompt = "「${riddle.description}」",
                feedback = "",
                childResponse = "",
                statusText = "第 ${s.round}/${s.totalRounds} 轮 · 猜猜我是谁？"
            )
        }

        scope.launch {
            // 🔊 念谜语
            if (s.round > 1) delay(300)
            speak(riddle.description)
            delay(400)
            speak("你知道这是什么吗？按住说话告诉我吧！")
            _state.update { it.copy(statusText = "按住说话回答 🎤") }
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
                statusText = "猜谜结束！",
                feedback = "得分：${s.score}\n猜对 ${s.score / 10} 个！再来一局？"
            )
        }

        // 🔊 结束语音
        scope.launch {
            speak("猜谜游戏结束啦！你一共猜对了${s.score / 10}个，得到$stars！你认识了好多小动物呢，太棒了！")
        }
    }

    override fun release() {
        stop()
        scope.cancel()
    }

    private data class Riddle(val description: String, val answers: List<String>)
}
