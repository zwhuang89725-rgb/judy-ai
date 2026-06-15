package com.pixelbuddy.app.presentation.voice

import androidx.lifecycle.ViewModel
import com.pixelbuddy.app.data.repository.AvatarRepository
import com.pixelbuddy.app.domain.model.AvatarConfig
import com.pixelbuddy.app.domain.usecase.AlwaysOnState
import com.pixelbuddy.app.domain.usecase.AlwaysOnVoiceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val alwaysOnUseCase: AlwaysOnVoiceUseCase,
    private val avatarRepository: AvatarRepository
) : ViewModel() {

    val state: StateFlow<AlwaysOnState> = alwaysOnUseCase.state

    val isActive: Boolean get() = state.value.isActive

    fun getAvatarConfig(): AvatarConfig = avatarRepository.getConfig()

    fun toggle() {
        if (state.value.isActive) {
            alwaysOnUseCase.stop()
        } else {
            alwaysOnUseCase.start()
        }
    }

    fun start() = alwaysOnUseCase.start()
    fun stop() = alwaysOnUseCase.stop()

    override fun onCleared() {
        super.onCleared()
        alwaysOnUseCase.release()
    }
}
