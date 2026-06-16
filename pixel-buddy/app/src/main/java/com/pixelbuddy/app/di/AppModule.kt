package com.pixelbuddy.app.di

import android.content.Context
import androidx.room.Room
import com.pixelbuddy.app.data.audio.AudioPlayer
import com.pixelbuddy.app.data.audio.AudioRecorder
import com.pixelbuddy.app.data.audio.StreamingAudioRecorder
import com.pixelbuddy.app.data.audio.VoiceActivityDetector
import com.pixelbuddy.app.data.repository.AvatarRepository
import com.pixelbuddy.app.presentation.theme.ThemePreferenceStore
import com.pixelbuddy.app.data.local.ApiKeyStore
import com.pixelbuddy.app.data.local.AppDatabase
import com.pixelbuddy.app.data.local.ChatDao
import com.pixelbuddy.app.data.local.ConfigDao
import com.pixelbuddy.app.data.remote.AIService
import com.pixelbuddy.app.data.remote.AIServiceImpl
import com.pixelbuddy.app.data.repository.ChatRepository
import com.pixelbuddy.app.data.repository.StoryRepository
import com.pixelbuddy.app.domain.usecase.AlwaysOnVoiceUseCase
import com.pixelbuddy.app.domain.usecase.ParentControlManager
import com.pixelbuddy.app.domain.usecase.VoiceChatUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── Database ──
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "pixel_buddy.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()
    @Provides fun provideConfigDao(db: AppDatabase): ConfigDao = db.configDao()

    // ── Secure Key Store ──
    @Provides @Singleton
    fun provideApiKeyStore(@ApplicationContext context: Context): ApiKeyStore =
        ApiKeyStore(context)

    // ── Theme Preference ──
    @Provides @Singleton
    fun provideThemePreferenceStore(@ApplicationContext context: Context): ThemePreferenceStore =
        ThemePreferenceStore(context)

    // ── AI Service ──
    @Provides @Singleton
    fun provideAIService(): AIService = AIServiceImpl()

    // ── Audio ──
    @Provides @Singleton
    fun provideAudioRecorder(@ApplicationContext context: Context): AudioRecorder =
        AudioRecorder(context)

    @Provides @Singleton
    fun provideAudioPlayer(@ApplicationContext context: Context): AudioPlayer =
        AudioPlayer(context)

    // ── Repository ──
    @Provides @Singleton
    fun provideAvatarRepository(@ApplicationContext context: Context): AvatarRepository =
        AvatarRepository(context)

    @Provides @Singleton
    fun provideStoryRepository(@ApplicationContext context: Context): StoryRepository =
        StoryRepository(context)

    @Provides @Singleton
    fun provideChatRepository(
        chatDao: ChatDao,
        configDao: ConfigDao,
        aiService: AIService,
        apiKeyStore: ApiKeyStore
    ): ChatRepository = ChatRepository(chatDao, configDao, aiService, apiKeyStore)

    // ── Streaming Audio ──
    @Provides @Singleton
    fun provideStreamingAudioRecorder(): StreamingAudioRecorder = StreamingAudioRecorder()

    @Provides @Singleton
    fun provideVoiceActivityDetector(): VoiceActivityDetector = VoiceActivityDetector()

    // ── Parent Control ──
    @Provides @Singleton
    fun provideParentControlManager(@ApplicationContext context: Context): ParentControlManager =
        ParentControlManager(context)

    // ── UseCases ──
    @Provides @Singleton
    fun provideVoiceChatUseCase(
        audioRecorder: AudioRecorder,
        audioPlayer: AudioPlayer,
        aiService: AIService,
        chatRepository: ChatRepository
    ): VoiceChatUseCase = VoiceChatUseCase(audioRecorder, audioPlayer, aiService, chatRepository)

    @Provides @Singleton
    fun provideAlwaysOnVoiceUseCase(
        streamingRecorder: StreamingAudioRecorder,
        audioPlayer: AudioPlayer,
        vad: VoiceActivityDetector,
        aiService: AIService,
        chatRepository: ChatRepository
    ): AlwaysOnVoiceUseCase = AlwaysOnVoiceUseCase(
        streamingRecorder, audioPlayer, vad, aiService, chatRepository
    )
}
