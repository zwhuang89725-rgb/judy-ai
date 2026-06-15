package com.pixelbuddy.app.presentation.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelbuddy.app.domain.model.AvatarConfig
import com.pixelbuddy.app.domain.model.AvatarType
import com.pixelbuddy.app.domain.model.PixelExpression
import com.pixelbuddy.app.presentation.theme.*

/**
 * 通用头像组件。
 * 根据 AvatarConfig 自动显示自定义照片或像素表情。
 *
 * @param config 头像配置
 * @param size 头像尺寸
 * @param showRing 是否显示外圈光环动画
 * @param ringColor 光环颜色
 * @param isActive 是否激活（控制光环脉冲）
 * @param modifier 额外修饰符
 */
@Composable
fun PixelAvatar(
    config: AvatarConfig,
    size: Int = 120,
    showRing: Boolean = false,
    ringColor: Color = PixelNeonGreen,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ringScale by rememberInfiniteTransition(label = "avatarRing").animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "ringScale"
    )

    Box(
        modifier = modifier.size((size * 1.25).dp),
        contentAlignment = Alignment.Center
    ) {
        // 光环
        if (showRing && isActive) {
            Box(
                modifier = Modifier
                    .size((size * ringScale).dp)
                    .clip(CircleShape)
                    .border(2.dp, ringColor.copy(alpha = 0.3f), CircleShape)
            )
        }

        // 头像主体
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(PixelSurface)
                .border(2.dp, ringColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (config.type) {
                AvatarType.CUSTOM_PHOTO -> {
                    val bitmap = remember(config.customImageUri) {
                        config.customImageUri?.let { BitmapFactory.decodeFile(it) }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "自定义头像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 加载失败回退
                        PixelExpressionFace(config.pixelExpression, Modifier.size((size * 0.65).dp))
                    }
                }
                AvatarType.PIXEL_DEFAULT -> {
                    PixelExpressionFace(config.pixelExpression, Modifier.size((size * 0.65).dp))
                }
            }
        }
    }
}

/**
 * 像素表情绘制（纯 Compose，不依赖外部资源）。
 */
@Composable
fun PixelExpressionFace(expression: PixelExpression, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (expression) {
            PixelExpression.HAPPY -> HappyFace()
            PixelExpression.THINKING -> ThinkingFace(true)
            PixelExpression.TALKING -> TalkingFace()
            PixelExpression.SLEEPY -> SleepyFace()
            PixelExpression.EXCITED -> ExcitedFace()
            PixelExpression.LOVING -> LovingFace()
            PixelExpression.SURPRISED -> SurprisedFace()
            PixelExpression.SILLY -> SillyFace()
        }
    }
}

// ═══ 表情实现（与 VoiceScreen 共用） ═══

@Composable
fun HappyFace() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(12.dp).background(PixelNeonGreen, RoundedCornerShape(2.dp)))
            Box(Modifier.size(12.dp).background(PixelNeonGreen, RoundedCornerShape(2.dp)))
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth(0.7f).height(8.dp).background(PixelYellow, RoundedCornerShape(2.dp)))
    }
}

@Composable
fun ThinkingFace(isActive: Boolean) {
    val eyeOffset by rememberInfiniteTransition(label = "think").animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "eyeOff"
    )

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(12.dp).offset(x = eyeOffset.dp).background(PixelBlue, RoundedCornerShape(2.dp)))
            Box(Modifier.size(12.dp).offset(x = (-eyeOffset).dp).background(PixelBlue, RoundedCornerShape(2.dp)))
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.width(16.dp).height(8.dp).background(PixelYellow.copy(alpha = 0.5f), RoundedCornerShape(2.dp)))
    }
}

@Composable
fun TalkingFace() {
    val mouthH by rememberInfiniteTransition(label = "talk").animateFloat(
        initialValue = 6f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(180), RepeatMode.Reverse),
        label = "mouth"
    )
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(">", color = PixelNeonGreen, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
            Text("<", color = PixelNeonGreen, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth(0.6f).height(mouthH.dp).background(PixelPink, RoundedCornerShape(2.dp)))
    }
}

@Composable
fun SleepyFace() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("–", color = PixelTextDim, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
            Text("–", color = PixelTextDim, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.width(24.dp).height(8.dp).background(PixelTextDim.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
    }
}

@Composable
fun ExcitedFace() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("*", color = PixelYellow, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
            Text("*", color = PixelYellow, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(0.8f).height(14.dp).background(PixelNeonGreen, RoundedCornerShape(2.dp)))
    }
}

@Composable
fun LovingFace() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("♥", color = PixelPink, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
            Text("♥", color = PixelPink, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.width(30.dp).height(10.dp).background(PixelPink.copy(alpha = 0.5f), RoundedCornerShape(2.dp)))
    }
}

@Composable
fun SurprisedFace() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("O", color = PixelYellow, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
            Text("O", color = PixelYellow, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.size(16.dp).background(PixelYellow, RoundedCornerShape(2.dp)))
    }
}

@Composable
fun SillyFace() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(">", color = PixelNeonGreen, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
            Text(">", color = PixelNeonGreen, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(0.7f).height(8.dp).background(PixelPink, RoundedCornerShape(2.dp)))
    }
}
