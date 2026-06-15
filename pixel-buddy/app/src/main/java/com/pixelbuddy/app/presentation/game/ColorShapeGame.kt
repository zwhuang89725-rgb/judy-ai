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
 * 颜色形状认知游戏 — 完整语音引导版。
 *
 * AI 全程用语音引导 + 画面同步显示 emoji 色块。
 * 开场说明 → 逐轮描述 → 鼓励/教导 → 结束总结。
 */
class ColorShapeGame(
    private val aiService: AIService,
    private val audioPlayer: AudioPlayer,
    private val chatRepository: ChatRepository
) : GameSession {

    override val game = Game(
        id = GameType.COLOR_SHAPE.name,
        type = GameType.COLOR_SHAPE,
        title = "颜色形状",
        description = "认识颜色和形状，答对有奖励！",
        iconEmoji = "🎨",
        difficulty = GameDifficulty.EASY
    )

    private val _state = MutableStateFlow(GamePlayState(totalRounds = 3, statusText = "准备开始"))
    override val state: StateFlow<GamePlayState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val items = listOf(
        ColorShapeItem("红色的、圆圆的东西，吃起来甜甜的，长在树上。是什么？", "🔴⭕", listOf("苹果", "红苹果")),
        ColorShapeItem("黄色的、弯弯的像月亮一样，剥开皮就能吃。是什么？", "🟡🌙", listOf("香蕉", "黄香蕉")),
        ColorShapeItem("橙色的、圆圆的，吃起来酸酸甜甜，可以榨成果汁。是什么？", "🟠⭕", listOf("橙子", "橘子", "桔子")),
        ColorShapeItem("绿色的、三角形的，吃起来甜甜的，夏天吃特别解渴。是什么？", "🟢🔺", listOf("西瓜", "绿西瓜")),
        ColorShapeItem("紫色的、小小圆圆的，一串一串的，可以做成葡萄干。是什么？", "🟣⭕", listOf("葡萄", "紫葡萄")),
        ColorShapeItem("蓝色的、方方的，白天在天上，晚上就看不见了。是什么？", "🔵🟦", listOf("天空", "蓝天")),
        ColorShapeItem("白色的、圆圆的，在冬天从天上飘下来，到手心就化了。是什么？", "⚪⭕", listOf("雪花", "雪")),
        ColorShapeItem("黄色的、圆圆的，挂在天上照得人暖暖的。是什么？", "🟡⭕", listOf("太阳", "大太阳")),
        ColorShapeItem("五颜六色的、弯弯的像一座桥，雨过天晴就能看到。是什么？", "🌈🌉", listOf("彩虹")),
        ColorShapeItem("绿色的、尖尖的，圣诞节的时候会挂满礼物。是什么？", "🟢🔺", listOf("圣诞树", "树")),
        ColorShapeItem("红色的、方方的，上面有白色的窗户，是消防员工作的地方。是什么？", "🔴🟥", listOf("消防车", "救火车")),
        ColorShapeItem("黄色的、有五个尖尖角，晚上在天上亮晶晶。是什么？", "🟡⭐", listOf("星星", "五角星")),
        ColorShapeItem("白色的、椭圆形的，母鸡每天都会下一个。是什么？", "⚪🥚", listOf("鸡蛋", "蛋")),
        ColorShapeItem("红色的、心形的，代表你对一个人的喜欢。是什么？", "🔴❤️", listOf("爱心", "心")),
        ColorShapeItem("绿色的、椭圆形的，夏天池塘里飘在水面上，青蛙坐在上面。是什么？", "🟢🟢", listOf("荷叶", "莲叶")),
    )

    private var currentItem: ColorShapeItem? = null

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

        // 🎨 语音开场
        scope.launch {
            _state.update { it.copy(statusText = "游戏开始！") }
            speak("欢迎来到颜色形状游戏！我会描述一个东西的颜色和形状，你看屏幕上的图案，猜猜我说的是什么。一共三轮，开始吧！")
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
        val item = currentItem ?: return

        val isCorrect = item.answers.any { answer -> text.contains(answer) }
        val newScore = if (isCorrect) s.score + 10 else s.score

        val feedbackText: String
        val voiceFeedback: String

        if (isCorrect) {
            val encouragements = listOf(
                "答对啦！就是${item.answers.first()}！",
                "没错！${item.answers.first()}！你真会观察！",
                "完全正确！${item.answers.first()}！击掌！"
            )
            feedbackText = encouragements.random() + " ✨"
            voiceFeedback = encouragements.random()
        } else {
            feedbackText = "答案是「${item.answers.first()}」～${item.emoji}"
            voiceFeedback = "答案是「${item.answers.first()}」哦。你看屏幕上的图案，记住它的样子吧！"
        }

        _state.update {
            it.copy(
                childResponse = text,
                score = newScore,
                feedback = feedbackText,
                statusText = if (isCorrect) "🎉 答对了！" else "加油～"
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

        val item = items.random()
        currentItem = item

        _state.update {
            it.copy(
                prompt = item.description,
                feedback = "",
                childResponse = "",
                visualHint = item.emoji,
                statusText = "第 ${s.round}/${s.totalRounds} 轮 · ${item.emoji}"
            )
        }

        scope.launch {
            // 🎨 念题目 + 引导看屏幕
            if (s.round > 1) delay(300)
            speak("看屏幕上的图案。${item.description}")
            delay(300)
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
                statusText = "认物结束！",
                feedback = "得分：${s.score}\n答对 ${s.score / 10} 个！再来一局？"
            )
        }

        // 🎨 结束语音
        scope.launch {
            speak("游戏结束！你答对了${s.score / 10}个，获得$stars！现在你认识了好多颜色和形状，真是一个聪明的观察家！")
        }
    }

    override fun release() {
        stop()
        scope.cancel()
    }

    private data class ColorShapeItem(
        val description: String,
        val emoji: String,
        val answers: List<String>
    )
}
