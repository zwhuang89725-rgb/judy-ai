package com.pixelbuddy.app.presentation.game

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelbuddy.app.domain.model.Game
import com.pixelbuddy.app.domain.model.GameType
import com.pixelbuddy.app.presentation.theme.*

@Composable
fun GameScreen(viewModel: GameViewModel = hiltViewModel()) {
    val gameListState by viewModel.gameListState.collectAsState()

    if (gameListState.activeSession != null) {
        // 游戏中
        GamePlayScreen(viewModel)
    } else {
        // 游戏列表
        GameListScreen(viewModel)
    }
}

@Composable
fun GameListScreen(viewModel: GameViewModel) {
    val games = remember { GameRegistry.getSupportedGames() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "> SELECT GAME",
                style = MaterialTheme.typography.labelLarge,
                color = PixelNeonGreen
            )
        }

        items(games) { game ->
            GameEntryCard(game) {
                viewModel.startGame(game.type)
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PixelSurfaceLight.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔌", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "更多游戏开发中...",
                        style = MaterialTheme.typography.labelMedium,
                        color = PixelTextDim
                    )
                    Text(
                        "新增游戏只需实现 GameSession 接口\n然后在这里注册即可",
                        style = MaterialTheme.typography.labelSmall,
                        color = PixelTextDim.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GameEntryCard(game: Game, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = PixelSurface),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, PixelNeonGreen.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PixelSurfaceLight)
                    .border(1.dp, PixelNeonGreen.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(game.iconEmoji, fontSize = 28.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    game.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PixelNeonGreen,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    game.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelOnBackground
                )
            }
            Text(
                "[START]",
                style = MaterialTheme.typography.labelSmall,
                color = PixelYellow
            )
        }
    }
}

@Composable
fun GamePlayScreen(viewModel: GameViewModel) {
    val playState by viewModel.gamePlayState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelSurface)
                .border(1.dp, PixelNeonGreen.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.stopGame() },
                modifier = Modifier.size(36.dp)
            ) {
                Text("✕", color = PixelPink, fontSize = 18.sp)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    playState.statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = PixelNeonGreen
                )
            }
            // Score
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = PixelYellow.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, PixelYellow.copy(alpha = 0.3f))
            ) {
                Text(
                    "⭐ ${playState.score}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = PixelYellow,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(Modifier.weight(0.3f))

        // Visual hint (emoji for color/shape games)
        if (playState.visualHint.isNotBlank()) {
            Card(
                modifier = Modifier.padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(containerColor = PixelSurfaceLight),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    playState.visualHint,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 48.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // AI Prompt (what AI said)
        if (playState.prompt.isNotBlank()) {
            Card(
                modifier = Modifier.padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(containerColor = PixelSurfaceLight),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PixelBlue.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "> PIXEL-BUDDY 说:",
                        style = MaterialTheme.typography.labelSmall,
                        color = PixelBlue
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        playState.prompt,
                        style = MaterialTheme.typography.headlineMedium,
                        color = PixelOnBackground,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Child response
        if (playState.childResponse.isNotBlank()) {
            Card(
                modifier = Modifier.padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(containerColor = PixelSurface),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PixelPink.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "> 你说:",
                        style = MaterialTheme.typography.labelSmall,
                        color = PixelPink
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "「${playState.childResponse}」",
                        style = MaterialTheme.typography.titleLarge,
                        color = PixelOnBackground,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Feedback
        AnimatedVisibility(
            visible = playState.feedback.isNotBlank(),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier.padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (playState.feedback.contains("棒") || playState.feedback.contains("对"))
                        PixelNeonGreen.copy(alpha = 0.1f) else PixelYellow.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    playState.feedback,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = PixelNeonGreen,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(Modifier.weight(0.5f))

        // Voice input button
        if (playState.isRunning && !playState.isFinished) {
            VoiceGameButton(
                statusText = playState.statusText,
                onStartRecord = { viewModel.startRecording() },
                onStopRecord = { viewModel.stopRecording() }
            )

            Spacer(Modifier.height(12.dp))
            Text(
                "按住说话，松开发送",
                style = MaterialTheme.typography.labelSmall,
                color = PixelTextDim
            )
        }

        // Finished state
        if (playState.isFinished) {
            Button(
                onClick = { viewModel.startGame(viewModel.gameListState.value.activeSession?.game?.type ?: GameType.ECHO_SPEAK) },
                modifier = Modifier.padding(horizontal = 40.dp).height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PixelNeonGreen, contentColor = PixelBackground
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("[ 再来一局 ]", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelLarge, color = PixelBackground)
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { viewModel.stopGame() }) {
                Text("[ 返回列表 ]", fontFamily = FontFamily.Monospace, color = PixelTextDim)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun VoiceGameButton(
    statusText: String,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.4f),
        label = "gameBtn"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isPressed) PixelPink else PixelNeonGreen)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onStartRecord()
                        tryAwaitRelease()
                        isPressed = false
                        onStopRecord()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text("🎤", fontSize = 36.sp)
    }
}
