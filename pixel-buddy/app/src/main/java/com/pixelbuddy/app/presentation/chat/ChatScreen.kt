package com.pixelbuddy.app.presentation.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelbuddy.app.domain.model.ChatMessage
import com.pixelbuddy.app.domain.model.MessageRole
import com.pixelbuddy.app.domain.usecase.VoiceChatState
import com.pixelbuddy.app.presentation.theme.*

@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Status bar
        VoiceStatusBar(voiceState, viewModel)

        // Error banner
        if (uiState.errorMessage != null) {
            ErrorBanner(
                message = uiState.errorMessage!!,
                onDismiss = viewModel::dismissError,
                onRetry = viewModel::retryLastMessage
            )
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(msg)
            }
        }

        // Input area (text + voice button)
        ChatInputBar(
            inputText = inputText,
            voiceState = voiceState,
            isSending = uiState.isSending,
            onInputChanged = viewModel::onInputChanged,
            onSendText = viewModel::sendTextMessage,
            onStartRecord = viewModel::startRecording,
            onStopRecord = viewModel::stopRecording,
            onCancelRecord = viewModel::cancelRecording,
            onStopPlayback = viewModel::stopPlayback
        )
    }
}

@Composable
fun ErrorBanner(message: String, onDismiss: () -> Unit, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixelError.copy(alpha = 0.15f))
            .border(1.dp, PixelError.copy(alpha = 0.3f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⚠ ${message}",
            style = MaterialTheme.typography.labelSmall,
            color = PixelError,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text("重试", color = PixelError, style = MaterialTheme.typography.labelSmall)
        }
        TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text("✕", color = PixelError.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun VoiceStatusBar(state: VoiceChatState, viewModel: ChatViewModel) {
    if (state.statusText == "按住说话" && state.error == null) return

    val bgColor = when {
        state.error != null -> PixelError.copy(alpha = 0.15f)
        state.isRecording -> PixelPink.copy(alpha = 0.15f)
        state.isProcessing -> PixelBlue.copy(alpha = 0.15f)
        state.isPlaying -> PixelNeonGreen.copy(alpha = 0.15f)
        else -> PixelNeonGreen.copy(alpha = 0.08f)
    }
    val textColor = when {
        state.error != null -> PixelError
        state.isRecording -> PixelPink
        state.isProcessing -> PixelBlue
        else -> PixelNeonGreen
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.2f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.isRecording) {
            // Recording animation dots
            repeat(3) { i ->
                val dotAlpha by rememberInfiniteTransition(label = "dot$i").animateFloat(
                    initialValue = 0.3f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(400, delayMillis = i * 150),
                        RepeatMode.Reverse
                    ), label = "dotAlpha$i"
                )
                Box(
                    Modifier
                        .size(6.dp)
                        .padding(end = 4.dp)
                        .background(textColor.copy(alpha = dotAlpha), CircleShape)
                )
            }
        } else if (state.isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = PixelBlue,
                strokeWidth = 2.dp
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = state.error ?: state.statusText,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        if (state.isPlaying) {
            TextButton(
                onClick = { viewModel.stopPlayback() },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("[STOP]", style = MaterialTheme.typography.labelSmall, color = PixelPink)
            }
        }
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    voiceState: VoiceChatState,
    isSending: Boolean = false,
    onInputChanged: (String) -> Unit,
    onSendText: () -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onCancelRecord: () -> Unit,
    onStopPlayback: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixelSurface)
            .border(1.dp, PixelNeonGreen.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Text input (collapsed when recording or sending)
        if (!voiceState.isRecording && !isSending) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                enabled = !isSending,
                placeholder = {
                    Text("> 输入消息...", color = PixelTextDim, fontFamily = FontFamily.Monospace)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PixelOnBackground,
                    unfocusedTextColor = PixelOnBackground,
                    focusedBorderColor = PixelNeonGreen.copy(alpha = 0.3f),
                    unfocusedBorderColor = PixelNeonGreen.copy(alpha = 0.1f),
                    cursorColor = PixelNeonGreen,
                    disabledTextColor = PixelTextDim,
                    disabledBorderColor = PixelNeonGreen.copy(alpha = 0.05f)
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
        } else if (isSending) {
            // Loading indicator while AI is responding
            Row(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = PixelNeonGreen,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Pixel Buddy 正在输入...",
                    style = MaterialTheme.typography.bodySmall,
                    color = PixelTextDim
                )
            }
        }

        // Voice button with press-and-hold
        val voiceScale by animateFloatAsState(
            targetValue = if (voiceState.isRecording) 1.15f else 1f,
            animationSpec = spring(dampingRatio = 0.5f),
            label = "voiceScale"
        )

        val voiceBg = if (voiceState.isRecording) PixelPink else PixelNeonGreen

        Box(
            modifier = Modifier
                .size(52.dp)
                .scale(voiceScale)
                .clip(CircleShape)
                .background(voiceBg)
                .then(
                    if (voiceState.isRecording) {
                        // Recording ring animation
                        Modifier.border(
                            3.dp,
                            PixelPink.copy(alpha = 0.5f),
                            CircleShape
                        )
                    } else Modifier
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onStartRecord()
                            tryAwaitRelease()
                            onStopRecord()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (voiceState.isRecording) "[]" else "🎤",
                fontSize = 20.sp,
                color = PixelBackground
            )
        }

        // Send text button (only when text input is present and not recording)
        if (inputText.isNotBlank() && !voiceState.isRecording) {
            Spacer(Modifier.width(6.dp))
            FilledIconButton(
                onClick = onSendText,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = PixelNeonGreen.copy(alpha = 0.2f),
                    contentColor = PixelNeonGreen
                )
            ) {
                Text(">", fontSize = 16.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val bgColor = if (isUser) PixelSurfaceLight else PixelSurface
    val borderColor = if (isUser) PixelPink.copy(alpha = 0.3f) else PixelNeonGreen.copy(alpha = 0.3f)
    val alignment = if (isUser) Alignment.End else Alignment.Start

    // 流式消息的光标闪烁动画
    val cursorAlpha by if (message.isStreaming) {
        rememberInfiniteTransition(label = "cursor").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(500), RepeatMode.Reverse
            ), label = "cursorAlpha"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Text(
            text = if (isUser) "> PLAYER" else "> PIXEL-BUDDY",
            style = MaterialTheme.typography.labelSmall,
            color = if (isUser) PixelPink else PixelNeonGreen,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(2.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.content.ifEmpty { " " },
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelOnBackground,
                    maxLines = 20
                )
                // Streaming cursor
                if (message.isStreaming) {
                    Text(
                        text = "▌",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelNeonGreen.copy(alpha = cursorAlpha)
                    )
                }
            }
        }
    }
}
