package com.pixelbuddy.app.data.repository

import com.pixelbuddy.app.data.local.ApiKeyStore
import com.pixelbuddy.app.data.local.ChatDao
import com.pixelbuddy.app.data.local.toDomain
import com.pixelbuddy.app.data.local.toEntity
import com.pixelbuddy.app.data.local.ConfigDao
import com.pixelbuddy.app.data.local.toDomain as configToDomain
import com.pixelbuddy.app.data.local.toEntity as configToEntity
import com.pixelbuddy.app.data.remote.AIService
import com.pixelbuddy.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao,
    private val configDao: ConfigDao,
    private val aiService: AIService,
    private val apiKeyStore: ApiKeyStore
) {
    fun observeMessages(): Flow<List<ChatMessage>> =
        chatDao.observeAll().map { list -> list.map { it.toDomain() } }

    private val _streamingMessage = MutableStateFlow<ChatMessage?>(null)
    /** 当前正在流式输出的消息（仅内存中，未写入 DB） */
    val streamingMessage: StateFlow<ChatMessage?> = _streamingMessage.asStateFlow()

    /** 从 DB 构建完整的对话消息列表，转为 API 可用的格式 */
    private suspend fun buildConversationHistory(systemPrompt: String): List<Map<String, String>> {
        val history = mutableListOf<Map<String, String>>()
        history.add(mapOf("role" to "system", "content" to systemPrompt))
        val allMessages = chatDao.observeAll().first()
        for (msg in allMessages) {
            val domain = msg.toDomain()
            history.add(mapOf(
                "role" to when (domain.role) {
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                    MessageRole.SYSTEM -> "system"
                },
                "content" to domain.content
            ))
        }
        return history
    }

    /**
     * 发送文字消息（非流式）。
     * 保存用户消息 → 加载完整历史 → 调用 AI → 保存回复。
     */
    suspend fun sendMessage(
        content: String,
        config: ModelConfig
    ): Result<ChatMessage> {
        // Save user message
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = content
        )
        chatDao.insert(userMsg.toEntity())

        // Build full conversation history
        val history = buildConversationHistory(config.systemPrompt)

        val result = aiService.chat(config, history)

        return result.map { reply ->
            val aiMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = MessageRole.ASSISTANT,
                content = reply
            )
            chatDao.insert(aiMsg.toEntity())
            aiMsg
        }
    }

    /**
     * 发送文字消息（流式）。
     * 保存用户消息 → 加载完整历史 → 以 SSE 方式逐步接收 AI 回复。
     */
    suspend fun sendMessageStream(
        content: String,
        config: ModelConfig
    ) {
        // 1. Save user message
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = content
        )
        chatDao.insert(userMsg.toEntity())

        // 2. Build full conversation history
        val history = buildConversationHistory(config.systemPrompt)

        // 3. Create placeholder AI message
        val aiMsgId = UUID.randomUUID().toString()
        val sb = StringBuilder()
        val placeholder = ChatMessage(
            id = aiMsgId,
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true
        )
        _streamingMessage.value = placeholder

        // 4. Call streaming API
        aiService.chatStream(
            config = config,
            messages = history,
            onToken = { delta ->
                sb.append(delta)
                _streamingMessage.value = placeholder.copy(content = sb.toString())
            },
            onComplete = {
                val finalMsg = ChatMessage(
                    id = aiMsgId,
                    role = MessageRole.ASSISTANT,
                    content = sb.toString(),
                    isStreaming = false
                )
                chatDao.insert(finalMsg.toEntity())
                _streamingMessage.value = null
            },
            onError = { error ->
                val finalMsg = ChatMessage(
                    id = aiMsgId,
                    role = MessageRole.ASSISTANT,
                    content = sb.ifEmpty { "😅 抱歉，我走神了… ${error.message?.take(60)}" }.toString(),
                    isStreaming = false
                )
                chatDao.insert(finalMsg.toEntity())
                _streamingMessage.value = null
            }
        )
    }

    suspend fun getActiveConfig(): ModelConfig {
        val entity = configDao.get("active")
        val base = entity?.configToDomain() ?: ModelConfig.OPENAI_PRESET
        // Merge encrypted keys from secure store
        return base.copy(
            chatApiKey = getChatApiKey(entity),
            sttApiKey = getSttApiKey(entity),
            ttsApiKey = getTtsApiKey(entity)
        )
    }

    /** 从加密存储获取 Chat API Key（含 Room→Secure 一键迁移） */
    private fun getChatApiKey(entity: com.pixelbuddy.app.data.local.ModelConfigEntity?): String {
        val secure = apiKeyStore.chatApiKey
        if (secure.isNotBlank()) return secure
        // One-time migration from Room
        val roomKey = entity?.chatApiKey ?: ""
        if (roomKey.isNotBlank()) apiKeyStore.chatApiKey = roomKey
        return roomKey
    }

    private fun getSttApiKey(entity: com.pixelbuddy.app.data.local.ModelConfigEntity?): String {
        val secure = apiKeyStore.sttApiKey
        if (secure.isNotBlank()) return secure
        val roomKey = entity?.sttApiKey ?: ""
        if (roomKey.isNotBlank()) apiKeyStore.sttApiKey = roomKey
        return roomKey
    }

    private fun getTtsApiKey(entity: com.pixelbuddy.app.data.local.ModelConfigEntity?): String {
        val secure = apiKeyStore.ttsApiKey
        if (secure.isNotBlank()) return secure
        val roomKey = entity?.ttsApiKey ?: ""
        if (roomKey.isNotBlank()) apiKeyStore.ttsApiKey = roomKey
        return roomKey
    }

    suspend fun saveConfig(config: ModelConfig) {
        // Save non-sensitive fields to Room (API keys zeroed out)
        configDao.save(config.copy(
            id = "active",
            chatApiKey = "",
            sttApiKey = "",
            ttsApiKey = ""
        ).toEntity())
        // Save API keys to encrypted store
        apiKeyStore.chatApiKey = config.chatApiKey
        apiKeyStore.sttApiKey = config.sttApiKey
        apiKeyStore.ttsApiKey = config.ttsApiKey
    }

    /**
     * 测试 API 连接：发送一个简短请求验证 key 是否有效。
     */
    suspend fun testConnection(config: ModelConfig) {
        aiService.chat(
            config,
            listOf(
                mapOf("role" to "user", "content" to "hi")
            )
        ).getOrThrow()
    }

    suspend fun clearHistory() {
        chatDao.clearAll()
    }
}
