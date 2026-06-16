package com.pixelbuddy.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.preferences.core.*
import com.pixelbuddy.app.domain.model.AvatarConfig
import com.pixelbuddy.app.domain.model.AvatarType
import com.pixelbuddy.app.domain.model.PixelExpression
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.getSharedPreferences("pixel_buddy_avatar", Context.MODE_PRIVATE)

    private val avatarsDir = File(context.filesDir, "avatars").apply { mkdirs() }

    companion object {
        private const val KEY_TYPE = "avatar_type"
        private const val KEY_EXPRESSION = "avatar_expression"
        private const val KEY_CUSTOM_URI = "avatar_custom_uri"
    }

    /**
     * 获取当前头像配置。
     */
    fun getConfig(): AvatarConfig {
        val typeStr = dataStore.getString(KEY_TYPE, AvatarType.PIXEL_DEFAULT.name)
            ?: AvatarType.PIXEL_DEFAULT.name
        val exprStr = dataStore.getString(KEY_EXPRESSION, PixelExpression.HAPPY.name)
            ?: PixelExpression.HAPPY.name
        val customUri = dataStore.getString(KEY_CUSTOM_URI, null)

        return AvatarConfig(
            type = try { AvatarType.valueOf(typeStr) } catch (_: Exception) { AvatarType.PIXEL_DEFAULT },
            pixelExpression = try { PixelExpression.valueOf(exprStr) } catch (_: Exception) { PixelExpression.HAPPY },
            customImageUri = customUri
        )
    }

    /**
     * 保存头像配置（同步，用于快速切换）。
     */
    fun saveConfig(config: AvatarConfig) {
        dataStore.edit {
            putString(KEY_TYPE, config.type.name)
            putString(KEY_EXPRESSION, config.pixelExpression.name)
            if (config.customImageUri != null) {
                putString(KEY_CUSTOM_URI, config.customImageUri)
            } else {
                remove(KEY_CUSTOM_URI)
            }
        }
    }

    /**
     * 切换像素表情。
     */
    fun setPixelExpression(expression: PixelExpression) {
        saveConfig(AvatarConfig(type = AvatarType.PIXEL_DEFAULT, pixelExpression = expression))
    }

    /**
     * 从 Uri 导入自定义头像照片。
     * 将图片缩放为 256×256 并保存到内部存储，返回持久化文件路径。
     */
    fun importCustomPhoto(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            // 缩放为正方形 256×256
            val size = minOf(bitmap.width, bitmap.height)
            val x = (bitmap.width - size) / 2
            val y = (bitmap.height - size) / 2
            val cropped = Bitmap.createBitmap(bitmap, x, y, size, size)
            val scaled = Bitmap.createScaledBitmap(cropped, 256, 256, true)

            // 保存
            val file = File(avatarsDir, "custom_avatar_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.PNG, 90, out)
            }

            // 回收临时 bitmap
            if (cropped != scaled) cropped.recycle()
            if (bitmap != cropped && bitmap != scaled) bitmap.recycle()

            val path = file.absolutePath
            saveConfig(AvatarConfig(type = AvatarType.CUSTOM_PHOTO, customImageUri = path))
            path
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取头像 Bitmap（用于显示）。
     * @param config 头像配置
     * @return Bitmap 或 null（像素表情返回 null，需用 Compose 绘制）
     */
    fun loadAvatarBitmap(config: AvatarConfig): Bitmap? {
        if (config.type != AvatarType.CUSTOM_PHOTO) return null
        val path = config.customImageUri ?: return null
        return try {
            BitmapFactory.decodeFile(path)
        } catch (_: Exception) { null }
    }

    /**
     * 删除自定义头像（回退到像素表情）。
     */
    fun removeCustomPhoto() {
        val config = getConfig()
        config.customImageUri?.let { File(it).delete() }
        saveConfig(AvatarConfig(type = AvatarType.PIXEL_DEFAULT, pixelExpression = config.pixelExpression))
    }
}

private fun android.content.SharedPreferences.edit(block: android.content.SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.block()
    editor.apply()
}
