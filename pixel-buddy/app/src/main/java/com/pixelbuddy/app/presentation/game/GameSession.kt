package com.pixelbuddy.app.presentation.game

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.pixelbuddy.app.domain.model.Game
import com.pixelbuddy.app.domain.model.GameType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 游戏运行时的统一状态。所有游戏共用此结构。
 */
data class GamePlayState(
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val round: Int = 0,
    val totalRounds: Int = 0,
    val score: Int = 0,
    val feedback: String = "",       // 给小朋友的反馈文字
    val prompt: String = "",         // AI 说的提示/问题
    val childResponse: String = "",  // 小孩的回答
    val visualHint: String = "",     // 画面辅助（emoji 色块等）
    val statusText: String = "准备开始"
)

/**
 * 游戏会话抽象接口。
 *
 * 每个游戏实现此接口，GameScreen 通过它驱动游戏生命周期。
 * 新增游戏只需：
 *   1. 创建一个类实现 [GameSession]
 *   2. 在 [GameRegistry] 中注册
 *   3. 游戏界面由 GameScreen 统一渲染（通过 state 驱动）
 */
interface GameSession {
    /** 游戏元信息 */
    val game: Game

    /** 运行时状态流 */
    val state: StateFlow<GamePlayState>

    /** 开始游戏 */
    fun start()

    /** 停止/退出游戏 */
    fun stop()

    /**
     * 处理用户输入（语音识别结果或按钮点击）。
     * @param text 识别文字或按钮标签
     */
    fun onUserInput(text: String)

    /** 下一轮（部分游戏支持） */
    fun nextRound()

    /** 释放资源 */
    fun release()
}

/**
 * 游戏会话工厂 — 每个游戏类型对应一个创建函数。
 * 新增游戏时在此注册。
 */
object GameRegistry {

    private val factories = mutableMapOf<GameType, () -> GameSession>()

    fun register(type: GameType, factory: () -> GameSession) {
        factories[type] = factory
    }

    fun create(type: GameType): GameSession? =
        factories[type]?.invoke()

    fun getSupportedGames(): List<Game> =
        GameType.entries.map { type ->
            Game(
                id = type.name,
                type = type,
                title = when (type) {
                    GameType.ECHO_SPEAK -> "跟读模仿"
                    GameType.SOUND_GUESS -> "声音猜谜"
                    GameType.COLOR_SHAPE -> "颜色形状"
                },
                description = when (type) {
                    GameType.ECHO_SPEAK -> "AI 说一个词，你来跟读！比谁说得像～"
                    GameType.SOUND_GUESS -> "听听这是什么声音？猜对得分！"
                    GameType.COLOR_SHAPE -> "认识颜色和形状，答对有奖励！"
                },
                iconEmoji = when (type) {
                    GameType.ECHO_SPEAK -> "🎤"
                    GameType.SOUND_GUESS -> "🔊"
                    GameType.COLOR_SHAPE -> "🎨"
                },
                difficulty = com.pixelbuddy.app.domain.model.GameDifficulty.EASY
            )
        }
}
