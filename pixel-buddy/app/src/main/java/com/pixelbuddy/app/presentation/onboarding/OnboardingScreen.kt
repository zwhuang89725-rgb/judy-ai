package com.pixelbuddy.app.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelbuddy.app.presentation.components.PixelExpressionFace
import com.pixelbuddy.app.domain.model.PixelExpression
import com.pixelbuddy.app.presentation.theme.*

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // 完成 → 导航回主页
    LaunchedEffect(state.isDone) {
        if (state.isDone) onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ═══ Header ═══
            Spacer(Modifier.height(40.dp))

            // Pixel buddy mascot
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PixelSurface)
                    .border(2.dp, PixelNeonGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PixelExpressionFace(
                    expression = when (state.testResult) {
                        "ok" -> PixelExpression.LOVING
                        null -> PixelExpression.HAPPY
                        else -> PixelExpression.SURPRISED
                    },
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "PIXEL BUDDY",
                style = MaterialTheme.typography.labelLarge,
                color = PixelNeonGreen
            )
            Text(
                "首次设置向导",
                style = MaterialTheme.typography.labelMedium,
                color = PixelTextDim
            )

            Spacer(Modifier.height(8.dp))

            // ═══ Step Indicator ═══
            StepIndicator(currentStep = state.step)

            Spacer(Modifier.height(20.dp))

            // ═══ Step Content ═══
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                        }
                    },
                    label = "stepAnim"
                ) { step ->
                    when (step) {
                        0 -> WelcomeStep { viewModel.nextStep() }
                        1 -> PresetStep(
                            selected = state.selectedPreset,
                            onSelect = viewModel::selectPreset,
                            onNext = viewModel::nextStep
                        )
                        2 -> ApiKeyStep(
                            apiKey = state.apiKey,
                            selectedPreset = state.selectedPreset,
                            isTesting = state.isTesting,
                            testResult = state.testResult,
                            onKeyChange = viewModel::updateApiKey,
                            onTest = viewModel::testConnection,
                            onNext = { viewModel.saveAndFinish() },
                            onBack = viewModel::prevStep
                        )
                        3 -> DoneStep(
                            onStart = { viewModel.saveAndFinish() }
                        )
                    }
                }
            }

            // ═══ Bottom skip ═══
            if (state.step < 2) {
                TextButton(
                    onClick = { viewModel.skip() },
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Text(
                        "[ 跳过设置，以后再说 ]",
                        style = MaterialTheme.typography.labelSmall,
                        color = PixelTextDim
                    )
                }
            } else {
                Spacer(Modifier.height(56.dp))
            }
        }
    }
}

@Composable
fun StepIndicator(currentStep: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0..2) {
            Box(
                modifier = Modifier
                    .size(if (i == currentStep) 10.dp else 7.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            i < currentStep -> PixelNeonGreen
                            i == currentStep -> PixelNeonGreen
                            else -> PixelNeonGreen.copy(alpha = 0.2f)
                        }
                    )
            )
            if (i < 2) {
                Box(
                    Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .background(PixelNeonGreen.copy(alpha = 0.2f))
                )
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "> WELCOME",
            style = MaterialTheme.typography.labelLarge,
            color = PixelNeonGreen
        )

        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = PixelSurface),
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PixelNeonGreen.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "👾",
                    fontSize = 48.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "你好，地球小朋友！",
                    style = MaterialTheme.typography.titleMedium,
                    color = PixelOnBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "我是 Pixel Buddy，来自 8-bit 星系。\n" +
                        "我们会一起聊天、讲故事、玩游戏！\n\n" +
                        "只要简单两步设置，\n我就能开口和你说话啦 🎤",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelTextDim,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PixelNeonGreen,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                "[ 开始设置 ]",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelLarge,
                color = Color.Black
            )
        }
    }
}

@Composable
fun PresetStep(
    selected: String,
    onSelect: (String) -> Unit,
    onNext: () -> Unit
) {
    val presets = listOf(
        Triple("OpenAI", "🤖", "使用 OpenAI 的 GPT-4o、Whisper\n和 TTS 服务，效果最好"),
        Triple("MiMo-TTS", "🎵", "使用 MiMo-V2.5-TTS 免费语音\n配合任意 Chat API"),
        Triple("Custom", "⚙️", "自定义所有 API 端点\n完全自由的配置")
    )

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "> STEP 1/2",
            style = MaterialTheme.typography.labelLarge,
            color = PixelNeonGreen
        )
        Text(
            "选择 AI 服务商",
            style = MaterialTheme.typography.titleMedium,
            color = PixelOnBackground
        )
        Text(
            "推荐使用 OpenAI，效果最好",
            style = MaterialTheme.typography.labelMedium,
            color = PixelTextDim
        )

        Spacer(Modifier.height(16.dp))

        presets.forEach { (name, emoji, desc) ->
            val isSel = selected == name
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSel) PixelNeonGreen.copy(alpha = 0.1f) else PixelSurface
                ),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSel) PixelNeonGreen else PixelNeonGreen.copy(alpha = 0.15f)
                ),
                onClick = { onSelect(name) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium, color = PixelOnBackground)
                        Text(
                            desc,
                            style = MaterialTheme.typography.labelSmall,
                            color = PixelTextDim,
                            lineHeight = 16.sp
                        )
                    }
                    if (isSel) {
                        Text("[✔]", color = PixelNeonGreen, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PixelNeonGreen, contentColor = Color.Black),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("[ 下一步 ]", fontFamily = FontFamily.Monospace, color = Color.Black)
        }
    }
}

@Composable
fun ApiKeyStep(
    apiKey: String,
    selectedPreset: String,
    isTesting: Boolean,
    testResult: String?,
    onKeyChange: (String) -> Unit,
    onTest: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "> STEP 2/2",
            style = MaterialTheme.typography.labelLarge,
            color = PixelNeonGreen
        )
        Text(
            "输入 API Key",
            style = MaterialTheme.typography.titleMedium,
            color = PixelOnBackground
        )
        Text(
            if (selectedPreset == "MiMo-TTS")
                "MiMo-TTS 目前免费，可以跳过 Key\n直接点「开始使用」"
            else
                "在 ${selectedPreset} 平台获取 API Key\n粘贴到下方",
            style = MaterialTheme.typography.labelMedium,
            color = PixelTextDim,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(16.dp))

        // API Key input
        OutlinedTextField(
            value = apiKey,
            onValueChange = onKeyChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("sk-...", color = PixelTextDim, fontFamily = FontFamily.Monospace)
            },
            label = {
                Text("API Key", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
            },
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PixelOnBackground,
                unfocusedTextColor = PixelOnBackground,
                focusedBorderColor = PixelNeonGreen,
                unfocusedBorderColor = PixelNeonGreen.copy(alpha = 0.3f),
                focusedLabelColor = PixelNeonGreen,
                unfocusedLabelColor = PixelTextDim,
                cursorColor = PixelNeonGreen
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        // Test connection
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onTest,
                enabled = apiKey.isNotBlank() && !isTesting,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PixelBlue),
                border = androidx.compose.foundation.BorderStroke(1.dp, PixelBlue.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    if (isTesting) "[ 测试中... ]" else "[ 测试连接 ]",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(Modifier.width(12.dp))

            when (testResult) {
                "ok" -> Text("✅ 连接成功！", color = PixelNeonGreen, style = MaterialTheme.typography.labelMedium)
                null -> {}
                else -> Text(
                    testResult,
                    color = PixelError,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PixelTextDim),
                border = androidx.compose.foundation.BorderStroke(1.dp, PixelTextDim.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("[ 上一步 ]", fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PixelNeonGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("[ 开始使用 ]", fontFamily = FontFamily.Monospace, color = Color.Black)
            }
        }

        if (selectedPreset == "MiMo-TTS") {
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "[ 跳过，MiMo-TTS 免费无需 Key ]",
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelTextDim
                )
            }
        }
    }
}

@Composable
fun DoneStep(onStart: () -> Unit) {
    val scale by rememberInfiniteTransition(label = "done").animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "donePulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("👾", fontSize = 64.sp, modifier = Modifier.scale(scale))

        Spacer(Modifier.height(16.dp))

        Text(
            "一切就绪！",
            style = MaterialTheme.typography.headlineMedium,
            color = PixelNeonGreen,
            fontFamily = FontFamily.Monospace
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Pixel Buddy 已经准备好了\n我们可以开始聊天啦～",
            style = MaterialTheme.typography.bodyMedium,
            color = PixelTextDim,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PixelNeonGreen,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                "[ 🚀 进入 Pixel Buddy ]",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelLarge,
                color = Color.Black
            )
        }
    }
}
