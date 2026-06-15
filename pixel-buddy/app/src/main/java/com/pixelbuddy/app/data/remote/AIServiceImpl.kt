package com.pixelbuddy.app.data.remote

import com.pixelbuddy.app.domain.model.ModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIServiceImpl @Inject constructor() : AIService {

    /**
     * 为每个 API 调用动态构建 OkHttpClient，注入 API Key。
     * 如果 config 中 apiKey 为空则不添加认证头（适用于免费无鉴权的端点）。
     */
    private fun buildClient(apiKey: String): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // API Key 认证拦截器
        if (apiKey.isNotBlank()) {
            builder.addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", "Bearer $apiKey")
                    .build()
                chain.proceed(request)
            }
        }

        return builder.build()
    }

    private fun createRetrofit(baseUrl: String, apiKey: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl.ensureTrailingSlash())
        .client(buildClient(apiKey))
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    override suspend fun chat(
        config: ModelConfig,
        messages: List<Map<String, String>>
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val api = createRetrofit(config.chatBaseUrl, config.chatApiKey)
                .create(ChatApi::class.java)
            val response = api.chatCompletion(
                ChatRequest(
                    model = config.chatModel,
                    messages = messages.map {
                        ChatMessageDto(it["role"] ?: "user", it["content"] ?: "")
                    }
                )
            )
            response.choices?.firstOrNull()?.message?.content
                ?: throw IllegalStateException("No response from AI")
        }
    }

    override suspend fun chatStream(
        config: ModelConfig,
        messages: List<Map<String, String>>,
        onToken: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        chat(config, messages)
            .onSuccess { text ->
                text.forEachIndexed { i, _ ->
                    onToken(text.take(i + 1))
                    kotlinx.coroutines.delay(30)
                }
                onComplete()
            }
            .onFailure { onError(it) }
    }

    override suspend fun transcribe(
        config: ModelConfig,
        audioData: ByteArray,
        sampleRate: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val api = createRetrofit(config.sttBaseUrl, config.sttApiKey)
                .create(SttApi::class.java)
            val fileBody = audioData.toRequestBody("audio/wav".toMediaType())
            val modelBody = config.sttModel.toRequestBody("text/plain".toMediaType())
            api.transcribe(fileBody, modelBody).text
        }
    }

    override suspend fun synthesize(
        config: ModelConfig,
        text: String,
        voice: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val api = createRetrofit(config.ttsBaseUrl, config.ttsApiKey)
                .create(TtsApi::class.java)
            val response = api.synthesize(
                url = "${config.ttsBaseUrl}audio/speech".ensureTrailingSlash().trimEnd('/'),
                request = TtsRequest(model = config.ttsModel, input = text, voice = voice)
            )
            response.bytes()
        }
    }

    override suspend fun fetchModels(baseUrl: String, apiKey: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val api = createRetrofit(baseUrl, apiKey).create(ModelsApi::class.java)
                val response = api.listModels()
                response.data
                    ?.map { it.id }
                    ?.filter { !it.contains("whisper") && !it.contains("tts") && !it.contains("dall-e") }
                    ?.sortedBy { it }
                    ?: emptyList()
            }
        }

    private fun String.ensureTrailingSlash(): String =
        if (endsWith("/")) this else "$this/"
}
