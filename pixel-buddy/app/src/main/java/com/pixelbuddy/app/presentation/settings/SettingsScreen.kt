package com.pixelbuddy.app.presentation.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelbuddy.app.domain.model.ModelConfig
import com.pixelbuddy.app.presentation.theme.*

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("> CONFIG", style = MaterialTheme.typography.labelLarge, color = PixelNeonGreen)

        // ═══ Avatar ═══
        SettingsSection("AVATAR") {
            AvatarSettingsSection(viewModel.avatarRepository)
        }

        // ═══════════ Chat 预设 ═══════════
        SettingsSection("💬 对话模型 (Chat) · OpenAI 兼容") {
            PresetChips(
                presets = ModelConfig.CHAT_PRESETS.map { it.name },
                selected = state.chatPreset,
                onSelect = { name ->
                    ModelConfig.CHAT_PRESETS.find { it.name == name }?.let {
                        viewModel.selectChatPreset(it)
                    }
                }
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelTextField(
                    value = state.chatBaseUrl,
                    onValueChange = viewModel::updateChatUrl,
                    modifier = Modifier.weight(1f),
                    label = "Base URL",
                    enabled = viewModel.isChatCustom
                )
                // 刷新模型按钮
                OutlinedButton(
                    onClick = { viewModel.fetchModels() },
                    modifier = Modifier.height(56.dp),
                    enabled = !state.isFetchingModels && state.chatBaseUrl.isNotBlank(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PixelBlue),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PixelBlue.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        if (state.isFetchingModels) "..." else "🔄",
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Model dropdown
            Box {
                PixelTextField(
                    value = state.chatModel,
                    onValueChange = viewModel::updateChatModel,
                    modifier = Modifier.fillMaxWidth(),
                    label = "Model",
                    enabled = viewModel.isChatCustom,
                    trailingIcon = {
                        if (state.availableModels.isNotEmpty()) {
                            TextButton(onClick = { viewModel.toggleModelDropdown() }) {
                                Text("▼", color = PixelNeonGreen, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                )

                DropdownMenu(
                    expanded = state.showModelDropdown && state.availableModels.isNotEmpty(),
                    onDismissRequest = { viewModel.toggleModelDropdown() }
                ) {
                    state.availableModels.take(30).forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Text(model, fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (model == state.chatModel) PixelNeonGreen else PixelOnBackground)
                            },
                            onClick = { viewModel.selectModel(model) }
                        )
                    }
                    if (state.availableModels.size > 30) {
                        DropdownMenuItem(
                            text = { Text("...还有 ${state.availableModels.size - 30} 个", color = PixelTextDim) },
                            onClick = {}, enabled = false
                        )
                    }
                }
            }

            if (state.modelFetchError != null) {
                Text(state.modelFetchError!!, style = MaterialTheme.typography.labelSmall, color = PixelError)
            }

            PixelTextField(
                value = state.chatApiKey, onValueChange = viewModel::updateChatKey,
                modifier = Modifier.fillMaxWidth(), label = "API Key", isPassword = true
            )
        }

        // ═══════════ STT 预设 ═══════════
        SettingsSection("🎙️ 语音识别 (STT)") {
            PresetChips(
                presets = ModelConfig.STT_PRESETS.map { it.name },
                selected = state.sttPreset,
                onSelect = { name ->
                    ModelConfig.STT_PRESETS.find { it.name == name }?.let {
                        viewModel.selectSttPreset(it)
                    }
                }
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelTextField(
                    value = state.sttBaseUrl, onValueChange = {},
                    modifier = Modifier.weight(1f), label = "URL", enabled = false
                )
                PixelTextField(
                    value = state.sttModel, onValueChange = {},
                    modifier = Modifier.weight(0.8f), label = "Model", enabled = false
                )
            }
            PixelTextField(
                value = state.sttApiKey, onValueChange = viewModel::updateSttKey,
                modifier = Modifier.fillMaxWidth(), label = "API Key", isPassword = true
            )
        }

        // ═══════════ TTS 预设 ═══════════
        SettingsSection("🔊 语音合成 (TTS)") {
            PresetChips(
                presets = ModelConfig.TTS_PRESETS.map { it.name },
                selected = state.ttsPreset,
                onSelect = { name ->
                    ModelConfig.TTS_PRESETS.find { it.name == name }?.let {
                        viewModel.selectTtsPreset(it)
                    }
                }
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelTextField(
                    value = state.ttsBaseUrl, onValueChange = {},
                    modifier = Modifier.weight(1f), label = "URL", enabled = false
                )
                PixelTextField(
                    value = state.ttsModel, onValueChange = {},
                    modifier = Modifier.weight(0.8f), label = "Model", enabled = false
                )
            }
            PixelTextField(
                value = state.ttsApiKey, onValueChange = viewModel::updateTtsKey,
                modifier = Modifier.fillMaxWidth(), label = "API Key", isPassword = true
            )
            if (state.ttsPreset.contains("小米 MiMo")) {
                Text(
                    "💡 小米 MiMo TTS 有免费额度，注册获取 Key 即可。若无 Key 也可留空尝试。",
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelYellow.copy(alpha = 0.7f)
                )
            }
        }

        // ═══════════ System Prompt ═══════════
        SettingsSection("🤖 AI 角色设定") {
            PixelTextField(
                value = state.systemPrompt, onValueChange = viewModel::updateSystemPrompt,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                label = "System Prompt", singleLine = false
            )
        }

        // ═══ Info ═══
        Text(
            "💡 当前：${state.chatPreset} + ${state.sttPreset} + ${state.ttsPreset}",
            style = MaterialTheme.typography.labelSmall,
            color = PixelTextDim
        )

        if (state.saveMessage != null) {
            Text(state.saveMessage!!, style = MaterialTheme.typography.labelMedium,
                color = PixelNeonGreen, fontFamily = FontFamily.Monospace)
        }

        Button(
            onClick = { viewModel.saveConfig() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
            colors = ButtonDefaults.buttonColors(
                containerColor = PixelNeonGreen, contentColor = PixelBackground,
                disabledContainerColor = PixelNeonGreen.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                if (state.isSaving) "[ 保存中... ]" else "[ SAVE CONFIG ]",
                fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelLarge,
                color = PixelBackground
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun PresetChips(presets: List<String>, selected: String, onSelect: (String) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(presets.size) { i ->
            val name = presets[i]
            val isSel = name == selected
            FilterChip(
                selected = isSel,
                onClick = { onSelect(name) },
                label = { Text(name, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PixelNeonGreen.copy(alpha = 0.2f),
                    selectedLabelColor = PixelNeonGreen
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = PixelNeonGreen.copy(alpha = 0.3f),
                    selectedBorderColor = PixelNeonGreen, enabled = true, selected = isSel
                )
            )
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text("> $title", style = MaterialTheme.typography.labelSmall, color = PixelYellow,
        modifier = Modifier.padding(top = 4.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PixelSurface),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PixelNeonGreen.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

@Composable
private fun PixelTextField(
    value: String, onValueChange: (String) -> Unit,
    modifier: Modifier, label: String,
    isPassword: Boolean = false, singleLine: Boolean = true, enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    TextField(
        value = value, onValueChange = onValueChange,
        modifier = modifier, enabled = enabled,
        label = { Text(label, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall) },
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = PixelOnBackground),
        trailingIcon = trailingIcon,
        visualTransformation = if (isPassword) PasswordVisualTransformation()
            else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PixelOnBackground, unfocusedTextColor = PixelOnBackground,
            focusedBorderColor = PixelNeonGreen, unfocusedBorderColor = PixelNeonGreen.copy(alpha = 0.25f),
            focusedLabelColor = PixelNeonGreen, unfocusedLabelColor = PixelTextDim,
            cursorColor = PixelNeonGreen,
            disabledTextColor = PixelOnBackground, disabledBorderColor = PixelNeonGreen.copy(alpha = 0.15f),
            disabledLabelColor = PixelTextDim
        ),
        singleLine = singleLine
    )
}
