package com.pixelbuddy.app.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.http.*

/**
 * OpenAI-compatible Chat Completions API
 */
interface ChatApi {
    @POST("chat/completions")
    suspend fun chatCompletion(@Body request: ChatRequest): ChatResponse
}

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean = false,
    val temperature: Float = 0.8f,
    @SerializedName("max_tokens") val maxTokens: Int = 512
)

data class ChatMessageDto(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>?
)

data class Choice(
    val message: ChatMessageDto?,
    val delta: ChatMessageDto?  // streaming
)

/**
 * OpenAI-compatible Speech-to-Text API
 */
interface SttApi {
    @Multipart
    @POST("audio/transcriptions")
    suspend fun transcribe(
        @Part file: okhttp3.RequestBody,
        @Part("model") model: okhttp3.RequestBody,
        @Part("language") language: okhttp3.RequestBody = "zh".toRequestBody()
    ): SttResponse
}

data class SttResponse(val text: String)

/**
 * TTS API — 通用接口，适配 MiMo-TTS 等
 */
interface TtsApi {
    @POST
    suspend fun synthesize(
        @Url url: String,
        @Body request: TtsRequest
    ): okhttp3.ResponseBody
}

data class TtsRequest(
    val model: String,
    val input: String,
    val voice: String = "default"
)

/**
 * OpenAI-compatible Models list API
 */
interface ModelsApi {
    @GET("models")
    suspend fun listModels(): ModelsResponse
}

data class ModelsResponse(
    val data: List<ModelInfo>?
)

data class ModelInfo(
    val id: String,
    val owned_by: String? = null
)

private fun String.toRequestBody(): okhttp3.RequestBody =
    okhttp3.RequestBody.create("text/plain".toMediaType(), this)
