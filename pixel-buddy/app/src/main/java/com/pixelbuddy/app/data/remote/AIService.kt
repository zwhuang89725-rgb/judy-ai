package com.pixelbuddy.app.data.remote

import com.pixelbuddy.app.domain.model.ModelConfig

/**
 * 多模型 AI 服务统一接口。
 * 所有 AI 调用（Chat / STT / TTS）都通过此接口，实现层按 ModelConfig 切换端点。
 */
interface AIService {

    /**
     * 流式 AI 聊天：发送消息列表，返回逐 token 流。
     */
    suspend fun chatStream(
        config: ModelConfig,
        messages: List<Map<String, String>>,
        onToken: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    )

    /**
     * 非流式 AI 聊天（用于简单场景）。
     */
    suspend fun chat(
        config: ModelConfig,
        messages: List<Map<String, String>>
    ): Result<String>

    /**
     * 语音识别：上传音频文件，返回识别文本。
     * @param audioData PCM/WAV 音频字节
     * @param sampleRate 采样率
     */
    suspend fun transcribe(
        config: ModelConfig,
        audioData: ByteArray,
        sampleRate: Int = 16000
    ): Result<String>

    /**
     * 文字转语音：输入文本，返回合成后的音频字节。
     * @return 音频数据（MP3/WAV）
     */
    suspend fun synthesize(
        config: ModelConfig,
        text: String,
        voice: String = "default"
    ): Result<ByteArray>

    /**
     * 获取可用模型列表（调 /v1/models）。
     */
    suspend fun fetchModels(baseUrl: String, apiKey: String): Result<List<String>>
}
