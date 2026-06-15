package com.pixelbuddy.app.domain.model

/**
 * 聊天消息数据模型
 */
data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

enum class MessageRole { USER, ASSISTANT, SYSTEM }

/**
 * 实时语音模式状态
 */
data class VoiceModeState(
    val isActive: Boolean = false,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val vadLevel: Float = 0f,            // 0..1 音量等级
    val lastTranscript: String = "",
    val isMuted: Boolean = false
)

/**
 * 头像配置
 */
data class AvatarConfig(
    val type: AvatarType = AvatarType.PIXEL_DEFAULT,
    val customImageUri: String? = null,   // 自定义照片本地 URI
    val pixelExpression: PixelExpression = PixelExpression.HAPPY
)

enum class AvatarType { PIXEL_DEFAULT, CUSTOM_PHOTO }

enum class PixelExpression {
    HAPPY,      // ^_^
    THINKING,   // o_O
    TALKING,    // >_<
    SLEEPY,     // -_-
    EXCITED,    // *o*
    LOVING,     // ♥_♥
    SURPRISED,  // O_O
    SILLY       // >_>
}

/**
 * 故事数据模型
 */
data class Story(
    val id: String,
    val title: String,
    val coverEmoji: String,
    val chapters: Int,
    val durationMinutes: Int,
    val ageRange: String,       // "3-6"
    val category: StoryCategory,
    val text: String,
    val isBuiltIn: Boolean = true
)

enum class StoryCategory { ADVENTURE, FAIRY_TALE, ANIMAL, SPACE, BEDTIME }

/**
 * 小游戏数据模型
 */
data class Game(
    val id: String,
    val type: GameType,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val difficulty: GameDifficulty
)

enum class GameType {
    ECHO_SPEAK,     // 跟读模仿
    SOUND_GUESS,    // 声音猜谜
    COLOR_SHAPE     // 颜色形状认知
}

enum class GameDifficulty { EASY, MEDIUM }

/**
 * AI 模型配置
 */
data class ModelConfig(
    val id: String = "default",
    val name: String = "Custom",
    val chatBaseUrl: String = "https://api.deepseek.com/v1",
    val chatApiKey: String = "",
    val chatModel: String = "deepseek-chat",
    val sttBaseUrl: String = "",
    val sttApiKey: String = "",
    val sttModel: String = "paraformer-v2",
    val ttsBaseUrl: String = "",
    val ttsApiKey: String = "",
    val ttsModel: String = "mimo-v2.5-tts",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
) {
    companion object {
        val DEFAULT_SYSTEM_PROMPT = """
            你是 Pixel Buddy，一个来自 8-bit 星系的小朋友的好朋友。
            你温柔、耐心、有趣，喜欢用简单的语言和小朋友交流。
            你会用像素游戏风格的表达方式，偶尔说 "*BEEP*" 或 "*BOOP*"。
            回答简短有趣，适合 6 岁以下的小朋友。
        """.trimIndent()

        // ═══════════════════════════════════════
        //  国内 Chat 模型预设
        // ═══════════════════════════════════════

        data class ChatPreset(val name: String, val baseUrl: String, val model: String)

        val CHAT_PRESETS = listOf(
            ChatPreset("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat"),
            ChatPreset("DeepSeek-R1", "https://api.deepseek.com/v1", "deepseek-reasoner"),
            ChatPreset("通义千问 (Qwen)", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus"),
            ChatPreset("智谱 GLM-4", "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash"),
            ChatPreset("Moonshot (Kimi)", "https://api.moonshot.cn/v1", "moonshot-v1-8k"),
            ChatPreset("百川 (Baichuan)", "https://api.baichuan-ai.com/v1", "Baichuan4"),
            ChatPreset("硅基流动 (免费)", "https://api.siliconflow.cn/v1", "Qwen/Qwen2.5-7B-Instruct"),
            ChatPreset("⚙️ 自定义兼容端点", "", ""),
        )

        // ═══════════════════════════════════════
        //  国内 STT（语音识别）预设
        // ═══════════════════════════════════════

        data class SttPreset(val name: String, val baseUrl: String, val model: String)

        val STT_PRESETS = listOf(
            SttPreset("阿里云 Paraformer", "https://dashscope.aliyuncs.com/api/v1", "paraformer-v2"),
            SttPreset("阿里云 SenseVoice", "https://dashscope.aliyuncs.com/api/v1", "sensevoice-v1"),
            SttPreset("讯飞语音听写", "https://iat-api.xfyun.cn/v2", "iat"),
            SttPreset("火山引擎 ASR", "https://openspeech.bytedance.com/api/v1", "asr"),
            SttPreset("SiliconFlow Whisper", "https://api.siliconflow.cn/v1", "whisper-1"),
        )

        // ═══════════════════════════════════════
        //  国内 TTS（语音合成）预设
        // ═══════════════════════════════════════

        data class TtsPreset(val name: String, val baseUrl: String, val model: String)

        val TTS_PRESETS = listOf(
            TtsPreset("小米 MiMo 免费", "", "mimo-v2.5-tts"),
            TtsPreset("火山引擎 TTS", "https://openspeech.bytedance.com/api/v1", "volcano-tts"),
            TtsPreset("阿里云 CosyVoice", "https://dashscope.aliyuncs.com/api/v1", "cosyvoice-v1"),
            TtsPreset("阿里云 Sambert", "https://dashscope.aliyuncs.com/api/v1", "sambert-zhichu-v1"),
            TtsPreset("讯飞语音合成", "https://tts-api.xfyun.cn/v2", "tts"),
            TtsPreset("硅基流动 FishAudio", "https://api.siliconflow.cn/v1", "fishaudio/fish-speech-1.4"),
        )

        // 兼容旧代码
        @Deprecated("Use CHAT_PRESETS/STT_PRESETS/TTS_PRESETS")
        val OPENAI_PRESET = ModelConfig(
            id = "openai", name = "OpenAI",
            chatBaseUrl = "https://api.openai.com/v1", chatModel = "gpt-4o",
            sttBaseUrl = "https://api.openai.com/v1", sttModel = "whisper-1"
        )

        @Deprecated("Use TTS_PRESETS")
        val MIMO_TTS_PRESET = ModelConfig(
            id = "mimo", name = "MiMo-TTS",
            ttsBaseUrl = "", ttsModel = "mimo-v2.5-tts"
        )
    }
}
