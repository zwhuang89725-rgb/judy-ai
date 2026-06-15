package com.pixelbuddy.app.presentation.story

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelbuddy.app.domain.model.Story
import com.pixelbuddy.app.domain.model.StoryCategory
import com.pixelbuddy.app.presentation.theme.*

@Composable
fun StoryScreen(viewModel: StoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 如果正在播放或睡前模式 → 显示播放器
        if (state.currentStory != null && (state.isPlaying || state.statusText.contains("暂停"))) {
            StoryPlayer(state, viewModel)
        } else if (state.isBedtimeMode) {
            BedtimeOverlay(state, viewModel)
        }

        // 分类筛选
        CategoryFilter(
            selected = state.selectedCategory,
            onSelect = { viewModel.selectCategory(it) }
        )

        // 故事列表或播放器
        if (state.currentStory != null && state.isPlaying) {
            // 小条指示当前播放
            NowPlayingMiniBar(state, viewModel)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (state.currentStory != null && state.statusText == "故事讲完啦 ✨") {
                item {
                    StoryFinishedCard(state.currentStory!!)
                }
            }

            items(state.stories, key = { it.id }) { story ->
                StoryCard(
                    story = story,
                    isPlaying = state.currentStory?.id == story.id && state.isPlaying,
                    onClick = { viewModel.playStory(story) }
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun StoryPlayer(state: StoryUiState, viewModel: StoryViewModel) {
    val story = state.currentStory ?: return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = PixelSurfaceLight),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PixelNeonGreen.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cover
            Text(story.coverEmoji, fontSize = 48.sp, textAlign = TextAlign.Center)

            Spacer(Modifier.height(8.dp))

            Text(
                story.title,
                style = MaterialTheme.typography.labelLarge,
                color = PixelNeonGreen,
                textAlign = TextAlign.Center
            )
            Text(
                "> 第 ${state.currentParagraphIndex + 1}/${state.totalParagraphs} 段 · ${story.durationMinutes} 分钟",
                style = MaterialTheme.typography.labelMedium,
                color = PixelTextDim
            )

            Spacer(Modifier.height(12.dp))

            // Progress
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = PixelNeonGreen,
                trackColor = PixelNeonGreen.copy(alpha = 0.1f)
            )

            Spacer(Modifier.height(12.dp))
            Text(
                state.statusText,
                style = MaterialTheme.typography.labelMedium,
                color = if (state.isPlaying) PixelNeonGreen else PixelYellow
            )

            Spacer(Modifier.height(16.dp))

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.stopPlayback() },
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, PixelError.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                ) {
                    Text("■", color = PixelError, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
                }

                FilledIconButton(
                    onClick = { viewModel.togglePause() },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = PixelNeonGreen, contentColor = PixelBackground
                    )
                ) {
                    Text(
                        if (state.isPlaying) "⏸" else "▶",
                        fontSize = 22.sp
                    )
                }

                // Bedtime button
                IconButton(
                    onClick = {
                        if (state.isBedtimeMode) viewModel.stopBedtimeMode()
                        else viewModel.startBedtimeMode()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .border(
                            1.dp,
                            if (state.isBedtimeMode) PixelYellow else PixelYellow.copy(alpha = 0.4f),
                            RoundedCornerShape(4.dp)
                        )
                ) {
                    Text("🌙", fontSize = 18.sp)
                }
            }

            // Bedtime indicator
            if (state.isBedtimeMode) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "睡前模式 · ${state.bedtimeRemaining / 60}:${String.format("%02d", state.bedtimeRemaining % 60)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelYellow
                )
            }
        }
    }
}

@Composable
fun BedtimeOverlay(state: StoryUiState, viewModel: StoryViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = PixelYellow.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PixelYellow.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🌙", fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "睡前模式",
                    style = MaterialTheme.typography.labelLarge,
                    color = PixelYellow
                )
                Text(
                    "剩余 ${state.bedtimeRemaining / 60}:${String.format("%02d", state.bedtimeRemaining % 60)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = PixelYellow.copy(alpha = 0.7f)
                )
            }
            TextButton(onClick = { viewModel.stopBedtimeMode() }) {
                Text("[取消]", style = MaterialTheme.typography.labelSmall, color = PixelTextDim)
            }
        }
    }
}

@Composable
fun NowPlayingMiniBar(state: StoryUiState, viewModel: StoryViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixelNeonGreen.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(state.currentStory?.coverEmoji ?: "", fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            state.currentStory?.title ?: "",
            style = MaterialTheme.typography.labelMedium,
            color = PixelNeonGreen,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = { viewModel.stopPlayback() },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("[停止]", style = MaterialTheme.typography.labelSmall, color = PixelPink)
        }
    }
}

@Composable
fun StoryFinishedCard(story: Story) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PixelNeonGreen.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PixelNeonGreen.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("✨", fontSize = 36.sp)
            Text(
                "「${story.title}」讲完啦！",
                style = MaterialTheme.typography.titleMedium,
                color = PixelNeonGreen
            )
            Text(
                "选一个新故事吧～",
                style = MaterialTheme.typography.labelMedium,
                color = PixelTextDim
            )
        }
    }
}

@Composable
fun StoryCard(story: Story, isPlaying: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (isPlaying) PixelNeonGreen.copy(alpha = 0.1f) else PixelSurface,
        animationSpec = tween(300),
        label = "storyBg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPlaying) PixelNeonGreen else PixelNeonGreen.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PixelSurfaceLight)
                    .border(1.dp, PixelNeonGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(story.coverEmoji, fontSize = 26.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPlaying) {
                        val dotAlpha by rememberInfiniteTransition(label = "playingDot").animateFloat(
                            initialValue = 1f, targetValue = 0.2f,
                            animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                            label = "dot"
                        )
                        Box(Modifier.size(6.dp).padding(end = 6.dp).background(PixelNeonGreen.copy(alpha = dotAlpha), CircleShape))
                    }
                    Text(
                        story.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isPlaying) PixelNeonGreen else PixelOnBackground
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${story.durationMinutes} 分钟 · ${story.ageRange} 岁",
                    style = MaterialTheme.typography.labelMedium,
                    color = PixelTextDim
                )
            }

            // Category badge
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = PixelNeonGreen.copy(alpha = 0.1f)
            ) {
                Text(
                    story.category.name,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelNeonGreen
                )
            }
        }
    }
}

@Composable
fun CategoryFilter(
    selected: StoryCategory?,
    onSelect: (StoryCategory?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixelSurface)
            .border(1.dp, PixelNeonGreen.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("ALL", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PixelNeonGreen.copy(alpha = 0.2f),
                    selectedLabelColor = PixelNeonGreen
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = PixelNeonGreen.copy(alpha = 0.3f),
                    selectedBorderColor = PixelNeonGreen,
                    enabled = true, selected = selected == null
                )
            )
        }
        items(StoryCategory.entries.toList()) { cat ->
            val isSel = selected == cat
            FilterChip(
                selected = isSel,
                onClick = { onSelect(if (isSel) null else cat) },
                label = {
                    Text(
                        when (cat) {
                            StoryCategory.BEDTIME -> "🌙 BEDTIME"
                            StoryCategory.FAIRY_TALE -> "✨ FAIRY"
                            StoryCategory.ADVENTURE -> "🚀 ADVENTURE"
                            StoryCategory.ANIMAL -> "🐾 ANIMAL"
                            StoryCategory.SPACE -> "🪐 SPACE"
                        },
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PixelNeonGreen.copy(alpha = 0.2f),
                    selectedLabelColor = PixelNeonGreen
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = PixelNeonGreen.copy(alpha = 0.3f),
                    selectedBorderColor = PixelNeonGreen,
                    enabled = true, selected = isSel
                )
            )
        }
    }
}
