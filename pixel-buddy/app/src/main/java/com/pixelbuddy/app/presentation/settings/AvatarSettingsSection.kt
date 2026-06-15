package com.pixelbuddy.app.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelbuddy.app.data.repository.AvatarRepository
import com.pixelbuddy.app.domain.model.AvatarConfig
import com.pixelbuddy.app.domain.model.AvatarType
import com.pixelbuddy.app.domain.model.PixelExpression
import com.pixelbuddy.app.presentation.components.PixelAvatar
import com.pixelbuddy.app.presentation.theme.*

/**
 * 头像设置区域组件。包含：
 * - 当前头像预览
 * - 从相册选择 / 拍照
 * - 8 种像素表情选择
 * - 删除自定义照片
 */
@Composable
fun AvatarSettingsSection(
    avatarRepository: AvatarRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var config by remember { mutableStateOf(avatarRepository.getConfig()) }

    // 相册选择器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = avatarRepository.importCustomPhoto(it)
            if (path != null) {
                config = avatarRepository.getConfig()
            }
        }
    }

    // 相机 (简化：复用相册逻辑，实际可用 TakePicture)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            // 保存到临时文件
            val file = java.io.File(context.cacheDir, "camera_avatar_${System.currentTimeMillis()}.png")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
            }
            val path = avatarRepository.importCustomPhoto(Uri.fromFile(file))
            if (path != null) {
                config = avatarRepository.getConfig()
            }
        }
    }

    Column(modifier = modifier.padding(12.dp)) {
        Text("> AVATAR CONFIG", style = MaterialTheme.typography.labelLarge, color = PixelNeonGreen)

        Spacer(Modifier.height(12.dp))

        // 当前头像预览
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelAvatar(
                config = config,
                size = 80,
                isActive = true,
                ringColor = if (config.type == AvatarType.CUSTOM_PHOTO) PixelPink else PixelNeonGreen
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    if (config.type == AvatarType.CUSTOM_PHOTO) "> 自定义照片" else "> 像素表情",
                    style = MaterialTheme.typography.titleMedium,
                    color = PixelNeonGreen
                )
                Text(
                    if (config.type == AvatarType.PIXEL_DEFAULT) "表情: ${config.pixelExpression.name}"
                    else "点击下方更换",
                    style = MaterialTheme.typography.labelMedium,
                    color = PixelTextDim
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 按钮行
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PixelNeonGreen),
                border = androidx.compose.foundation.BorderStroke(1.dp, PixelNeonGreen.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("[ 相册 ]", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = { cameraLauncher.launch(null) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PixelBlue),
                border = androidx.compose.foundation.BorderStroke(1.dp, PixelBlue.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("[ 拍照 ]", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
            }
            if (config.type == AvatarType.CUSTOM_PHOTO) {
                OutlinedButton(
                    onClick = {
                        avatarRepository.removeCustomPhoto()
                        config = avatarRepository.getConfig()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PixelError),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PixelError.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("[ 删除照片 ]", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // 像素表情选择
        Text(
            "> 选择像素表情",
            style = MaterialTheme.typography.labelSmall,
            color = PixelYellow
        )
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PixelExpression.entries.toList()) { expression ->
                val isSelected = config.type == AvatarType.PIXEL_DEFAULT && config.pixelExpression == expression
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) PixelNeonGreen.copy(alpha = 0.15f) else PixelSurface)
                        .border(
                            1.dp,
                            if (isSelected) PixelNeonGreen else PixelNeonGreen.copy(alpha = 0.15f),
                            RoundedCornerShape(4.dp)
                        )
                        .clickable {
                            avatarRepository.setPixelExpression(expression)
                            config = avatarRepository.getConfig()
                        }
                        .padding(8.dp)
                        .width(64.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PixelSurfaceLight),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelAvatar(
                            config = AvatarConfig(
                                type = AvatarType.PIXEL_DEFAULT,
                                pixelExpression = expression
                            ),
                            size = 40,
                            isActive = false
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        expression.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) PixelNeonGreen else PixelTextDim,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
