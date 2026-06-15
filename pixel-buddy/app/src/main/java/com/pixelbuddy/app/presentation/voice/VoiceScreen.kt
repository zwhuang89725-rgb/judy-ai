package com.pixelbuddy.app.presentation.voice

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelbuddy.app.domain.usecase.AlwaysOnState
import com.pixelbuddy.app.domain.model.PixelExpression
import com.pixelbuddy.app.data.audio.VoiceActivityDetector
import com.pixelbuddy.app.presentation.components.PixelAvatar
import com.pixelbuddy.app.presentation.theme.*

@Composable
fun VoiceScreen(viewModel: VoiceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val avatarConfig = remember { viewModel.getAvatarConfig() }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(0.5f))

        // ═══ 头像区域 ═══
        VoiceAvatarRing(state, avatarConfig)

        Spacer(Modifier.height(32.dp))

        // ═══ 音量波形 ═══
        AudioWaveform(
            energyLevel = state.energyLevel,
            isActive = state.isActive,
            isPlaying = state.isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .height(48.dp)
        )

        Spacer(Modifier.height(20.dp))

        // ═══ 状态文字 ═══
        VoiceStatusBadge(state)

        // ═══ 识别文字（如果有） ═══
        if (state.currentTranscript.isNotBlank() && !state.isProcessing) {
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.padding(horizontal = 24.dp),
                shape = RoundedCornerShape(4.dp),
                color = PixelSurfaceLight,
                border = androidx.compose.foundation.BorderStroke(1.dp, PixelBlue.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "> ${state.currentTranscript}",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelBlue,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "! ${state.error}",
                style = MaterialTheme.typography.labelSmall,
                color = PixelError
            )
        }

        Spacer(Modifier.weight(0.8f))

        // ═══ 开关按钮 ═══
        VoiceToggleButton(
            isActive = state.isActive,
            onClick = { viewModel.toggle() }
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = if (state.isActive) "> 像打电话一样，直接说话就好" else "> 点击开启实时语音模式",
            style = MaterialTheme.typography.labelMedium,
            color = PixelTextDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun VoiceAvatarRing(state: AlwaysOnState, avatarConfig: com.pixelbuddy.app.domain.model.AvatarConfig) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring")

    // 光环脉冲
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "ringScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 0.1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "ringAlpha"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            state.isPlaying -> PixelNeonGreen
            state.isProcessing -> PixelBlue
            state.vadState == VoiceActivityDetector.State.SPEAKING -> PixelPink
            state.vadState == VoiceActivityDetector.State.SPEECH_START -> PixelYellow
            state.isActive -> PixelNeonGreen.copy(alpha = 0.6f)
            else -> PixelTextDim
        },
        animationSpec = tween(300),
        label = "borderColor"
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .scale(if (state.isActive) ringScale else 1f)
            .clip(CircleShape)
            .background(if (state.isActive) PixelNeonGreen.copy(alpha = ringAlpha * 0.15f) else PixelSurfaceLight.copy(alpha = 0.3f))
            .border(3.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Inner avatar
        PixelAvatar(
            config = if (avatarConfig.type == com.pixelbuddy.app.domain.model.AvatarType.PIXEL_DEFAULT)
                avatarConfig.copy(pixelExpression = state.expression)
            else avatarConfig,
            size = 140,
            isActive = state.isActive,
            ringColor = borderColor,
            showRing = false,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun PixelExpressionFace(
    expression: PixelExpression,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (expression) {
            PixelExpression.HAPPY -> HappyFace()
            PixelExpression.THINKING -> ThinkingFace(isActive)
            PixelExpression.TALKING -> TalkingFace()
            PixelExpression.SLEEPY -> SleepyFace()
            PixelExpression.EXCITED -> ExcitedFace()
            PixelExpression.LOVING -> LovingFace()
            PixelExpression.SURPRISED -> SurprisedFace()
            PixelExpression.SILLY -> SillyFace()
        }
    }
}

// ═══ 8 种像素表情 ═══

@Composable
fun HappyFace() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        // Eyes: = =
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(Modifier.size(14.dp).background(PixelNeonGreen, RoundedCornerShape(2.dp)))
            Box(Modifier.size(14.dp).background(PixelNeonGreen, RoundedCornerShape(2.dp)))
        }
        Spacer(Modifier.height(16.dp))
        // Mouth: smile
        Box(Modifier.width(48.dp).height(10.dp).background(PixelYellow, RoundedCornerShape(2.dp)))
    }
}

@Composable
fun ThinkingFace(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "think")
    val eyeOffset by if (isActive) infiniteTransition.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "eyeOffset"
    ) else remember { mutableFloatStateOf(0f) }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(Modifier.size(14.dp).offset(x = eyeOffset.dp).background(PixelBlue, RoundedCornerShape(2.dp)))
            Box(Modifier.size(14.dp).offset(x = (-eyeOffset).dp).background(PixelBlue, RoundedCornerShape(2.dp)))
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.width(20.dp).height(10.dp).background(PixelYellow.copy(alpha = 0.5f), RoundedCornerShape(2.dp)))
    }
}

@Composable
fun TalkingFace() {
    val infiniteTransition = rememberInfiniteTransition(label = "talk")
    val mouthH by infiniteTransition.animateFloat(
        initialValue = 8f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(200), RepeatMode.Reverse),
        label = "mouthH"
    )

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        // Eyes: > <
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(">", color = PixelNeonGreen, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge)
            Text("<", color = PixelNeonGreen, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.width(40.dp).height(mouthH.dp).background(PixelPink, RoundedCornerShape(2.dp)))
    }
}

@Composable
fun SleepyFace() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("–", color = PixelTextDim, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge)
            Text("–", color = PixelTextDim, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.width(28.dp).height(10.dp).background(PixelTextDim.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
    }
}

@Composable
fun ExcitedFace() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("*", color = PixelYellow, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.headlineMedium)
            Text("*", color = PixelYellow, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.width(52.dp).height(16.dp).background(PixelNeonGreen, RoundedCornerShape(2.dp)))
    }
}

@Composable
fun LovingFace() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("♥", color = PixelPink, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.headlineMedium)
            Text("♥", color = PixelPink, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.width(36.dp).height(12.dp).background(PixelPink.copy(alpha = 0.5f), RoundedCornerShape(2.dp)))
    }
}

@Composable
fun SurprisedFace() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("O", color = PixelYellow, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.headlineMedium)
            Text("O", color = PixelYellow, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.size(18.dp).background(PixelYellow, RoundedCornerShape(2.dp)))
    }
}

@Composable
fun SillyFace() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(">", color = PixelNeonGreen, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge)
            Text(">", color = PixelNeonGreen, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.width(44.dp).height(10.dp).background(PixelPink, RoundedCornerShape(2.dp)))
    }
}

@Composable
fun AudioWaveform(
    energyLevel: Float,
    isActive: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 16) {
            val baseHeight = 0.15f + (i % 4) * 0.1f
            val waveOffset by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(600 + i * 40, delayMillis = i * 30),
                    RepeatMode.Reverse
                ), label = "wave$i"
            )

            val heightFraction = if (isActive) {
                (baseHeight + energyLevel * 0.7f + waveOffset * 0.2f).coerceIn(0.08f, 1f)
            } else {
                0.08f
            }

            val barColor = when {
                isPlaying && i % 3 == 0 -> PixelNeonGreen
                energyLevel > 0.4f && i % 2 == 0 -> PixelPink
                isActive -> PixelNeonGreen.copy(alpha = 0.5f)
                else -> PixelTextDim.copy(alpha = 0.2f)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(1.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun VoiceStatusBadge(state: AlwaysOnState) {
    val bgColor = when {
        state.isProcessing -> PixelBlue.copy(alpha = 0.15f)
        state.isPlaying -> PixelNeonGreen.copy(alpha = 0.15f)
        state.vadState == VoiceActivityDetector.State.SPEAKING -> PixelPink.copy(alpha = 0.15f)
        state.isActive -> PixelNeonGreen.copy(alpha = 0.08f)
        else -> PixelSurfaceLight.copy(alpha = 0.3f)
    }

    val textColor = when {
        state.isProcessing -> PixelBlue
        state.isPlaying -> PixelNeonGreen
        state.vadState == VoiceActivityDetector.State.SPEAKING -> PixelPink
        state.isActive -> PixelNeonGreen
        else -> PixelTextDim
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.isActive) {
                val dotAlpha by rememberInfiniteTransition(label = "statusDot").animateFloat(
                    initialValue = 1f, targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                    label = "dot"
                )
                Box(Modifier.size(8.dp).padding(end = 8.dp).background(textColor.copy(alpha = dotAlpha), CircleShape))
            }
            Text(
                text = state.statusText,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun VoiceToggleButton(isActive: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = 1f, animationSpec = spring(dampingRatio = 0.3f), label = "btnScale"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 40.dp)
            .height(56.dp)
            .scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) PixelError else PixelNeonGreen,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = if (isActive) "[ 挂断 ]" else "[ 开始实时语音 ]",
            style = MaterialTheme.typography.labelLarge,
            fontFamily = FontFamily.Monospace,
            color = Color.Black
        )
    }
}
