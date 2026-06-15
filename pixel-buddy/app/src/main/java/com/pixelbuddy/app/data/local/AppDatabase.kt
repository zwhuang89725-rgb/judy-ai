package com.pixelbuddy.app.data.local

import androidx.room.*
import com.pixelbuddy.app.domain.model.ChatMessage
import com.pixelbuddy.app.domain.model.MessageRole
import com.pixelbuddy.app.domain.model.ModelConfig
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [ChatMessageEntity::class, ModelConfigEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun configDao(): ConfigDao
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val role: String,
    val content: String,
    val timestamp: Long
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}

fun ChatMessageEntity.toDomain() = ChatMessage(
    id = id,
    role = MessageRole.valueOf(role),
    content = content,
    timestamp = timestamp
)

fun ChatMessage.toEntity() = ChatMessageEntity(
    id = id,
    role = role.name,
    content = content,
    timestamp = timestamp
)

@Entity(tableName = "model_configs")
data class ModelConfigEntity(
    @PrimaryKey val id: String,
    val name: String,
    val chatBaseUrl: String,
    val chatApiKey: String,
    val chatModel: String,
    val sttBaseUrl: String,
    val sttApiKey: String,
    val sttModel: String,
    val ttsBaseUrl: String,
    val ttsApiKey: String,
    val ttsModel: String,
    val systemPrompt: String
)

@Dao
interface ConfigDao {
    @Query("SELECT * FROM model_configs WHERE id = :id")
    suspend fun get(id: String = "active"): ModelConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(config: ModelConfigEntity)

    @Query("SELECT * FROM model_configs")
    suspend fun all(): List<ModelConfigEntity>
}

fun ModelConfigEntity.toDomain() = ModelConfig(
    id = id, name = name,
    chatBaseUrl = chatBaseUrl, chatApiKey = chatApiKey, chatModel = chatModel,
    sttBaseUrl = sttBaseUrl, sttApiKey = sttApiKey, sttModel = sttModel,
    ttsBaseUrl = ttsBaseUrl, ttsApiKey = ttsApiKey, ttsModel = ttsModel,
    systemPrompt = systemPrompt
)

fun ModelConfig.toEntity() = ModelConfigEntity(
    id = id, name = name,
    chatBaseUrl = chatBaseUrl, chatApiKey = chatApiKey, chatModel = chatModel,
    sttBaseUrl = sttBaseUrl, sttApiKey = sttApiKey, sttModel = sttModel,
    ttsBaseUrl = ttsBaseUrl, ttsApiKey = ttsApiKey, ttsModel = ttsModel,
    systemPrompt = systemPrompt
)
