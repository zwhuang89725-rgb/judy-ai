package com.pixelbuddy.app.data.repository

import com.pixelbuddy.app.data.local.ChatDao
import com.pixelbuddy.app.data.local.toDomain
import com.pixelbuddy.app.data.local.toEntity
import com.pixelbuddy.app.data.local.ConfigDao
import com.pixelbuddy.app.data.local.toDomain as configToDomain
import com.pixelbuddy.app.data.local.toEntity as configToEntity
import com.pixelbuddy.app.data.remote.AIService
import com.pixelbuddy.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao,
    private val configDao: ConfigDao,
    private val aiService: AIService
) {
    fun observeMessages(): Flow<List<ChatMessage>> =
        chatDao.observeAll().map { list -> list.map { it.toDomain() } }

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

        // Build conversation
        val allMessages = chatDao.observeAll()
        // (简化：用 Flow.first() 获取当前列表)
        val history = mutableListOf<Map<String, String>>()
        history.add(mapOf("role" to "system", "content" to config.systemPrompt))
        // 简化中…

        val result = aiService.chat(config, listOf(
            mapOf("role" to "system", "content" to config.systemPrompt),
            mapOf("role" to "user", "content" to content)
        ))

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

    suspend fun getActiveConfig(): ModelConfig {
        val entity = configDao.get("active")
        return entity?.configToDomain() ?: ModelConfig.OPENAI_PRESET
    }

    suspend fun saveConfig(config: ModelConfig) {
        configDao.save(config.copy(id = "active").toEntity())
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
