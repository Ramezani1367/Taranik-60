package com.tranik.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tranik.app.data.model.LyricLine
import com.tranik.app.ui.theme.*
import com.tranik.app.ui.viewmodel.PlayerViewModel
import com.tranik.app.ui.viewmodel.RepeatMode
import com.tranik.app.ui.components.QueueBottomSheet

@Composable
fun PlayerScreen(vm: PlayerViewModel) {
    val state by vm.state.collectAsState()
    val track = state.currentTrack
    val context = LocalContext.current
    var showQueue by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color(0xFF1A1328))) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("ترانیک پلیر", fontSize = 12.sp, color = Text4)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showQueue = true }) {
                Icon(Icons.Default.QueueMusic, contentDescription = "صف پخش", tint = Text2)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f).forEach { speed ->
                    val selected = state.playbackSpeed == speed
                    FilterChip(
                        selected = selected,
                        onClick = { vm.setPlaybackSpeed(speed) },
                        label = { Text("${speed}x", fontSize = 10.sp) },
                        modifier = Modifier.height(26.dp).padding(horizontal = 2.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent.copy(0.2f), selectedLabelColor = Accent)
                    )
                    Spacer(Modifier.width(3.dp))
                }
            }
        }

        if (track != null) {
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(track.displayName, fontSize = 15.sp, color = Text1, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${track.displayArtist} • ${track.displayAlbum}", fontSize = 12.sp, color = Text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${formatDuration(state.currentPositionMs)} / ${track.duration}", fontSize = 11.sp, color = Text4)
                }
                Box(Modifier.size(62.dp).clip(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    if (track.albumId >= 0) {
                        val albumArtUri = vm.getAlbumArtUri(track.albumId)
                        AsyncImage(model = ImageRequest.Builder(context).data(albumArtUri).crossfade(true).build(), contentDescription = "کاور آلبوم", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(Modifier.fillMaxSize().background(Color(0xFF2A2040)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, contentDescription = "آیکون موسیقی", tint = Accent.copy(0.6f), modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            Slider(value = state.progress, onValueChange = { vm.seekTo(it) }, valueRange = 0f..100f, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = DarkBg3))
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(state.currentPositionMs), fontSize = 12.sp, color = Text4)
                Text(track.duration, fontSize = 12.sp, color = Text4)
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.toggleShuffle() }) { Icon(Icons.Default.Shuffle, contentDescription = if (state.shuffleEnabled) "شافل روشن" else "شافل", tint = if (state.shuffleEnabled) Accent else Text2) }
                IconButton(onClick = { vm.skipPrevious() }) { Icon(Icons.Default.SkipPrevious, contentDescription = "قبلی", tint = Text2, modifier = Modifier.size(28.dp)) }
                FloatingActionButton(onClick = { vm.togglePlay() }, containerColor = Accent, modifier = Modifier.size(60.dp)) { Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (state.isPlaying) "مکث" else "پخش", tint = Color.White, modifier = Modifier.size(28.dp)) }
                IconButton(onClick = { vm.skipNext() }) { Icon(Icons.Default.SkipNext, contentDescription = "بعدی", tint = Text2, modifier = Modifier.size(28.dp)) }
                IconButton(onClick = { vm.cycleRepeatMode() }) { Icon(when (state.repeatMode) { RepeatMode.ONE -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat }, contentDescription = when (state.repeatMode) { RepeatMode.OFF -> "تکرار خاموش"; RepeatMode.ALL -> "تکرار همه"; RepeatMode.ONE -> "تکرار یکی" }, tint = when (state.repeatMode) { RepeatMode.OFF -> Text2; else -> Accent }) }
            }
            Spacer(Modifier.height(12.dp))

            if (state.karaokeLines.isNotEmpty()) {
                KaraokeSection(lines = state.karaokeLines, activeIndex = state.activeKaraokeIndex, modifier = Modifier.weight(1f))
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "لیست آهنگ‌ها", modifier = Modifier.size(64.dp), tint = Text4)
                    Spacer(Modifier.height(12.dp))
                    Text("آهنگی در حال پخش نیست", color = Text3, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("از کتابخانه آهنگ انتخاب کنید", color = Text4, fontSize = 12.sp)
                }
            }
        }
    }

    if (showQueue && state.queue.isNotEmpty()) {
        QueueBottomSheet(queue = state.queue, currentIndex = state.queueIndex, isPlaying = state.isPlaying, shuffleEnabled = state.shuffleEnabled, repeatMode = state.repeatMode.name, onTrackClick = { index ->
            val queuedTrack = state.queue.getOrNull(index)
            if (queuedTrack != null) { vm.playTrack(queuedTrack, context, state.queue) }
        }, onDismiss = { showQueue = false })
    }
}

@Composable
private fun KaraokeSection(lines: List<LyricLine>, activeIndex: Int, modifier: Modifier = Modifier) {
    val lazyListState = rememberLazyListState()
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) { lazyListState.animateScrollToItem(activeIndex) }
    }
    LazyColumn(modifier.padding(horizontal = 18.dp), state = lazyListState, horizontalAlignment = Alignment.CenterHorizontally) {
        itemsIndexed(lines) { i, line ->
            val isActive = i == activeIndex
            val fontSize by animateFloatAsState(targetValue = if (isActive) 20f else 14f, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "karaoke")
            Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(12.dp)).background(if (isActive) Accent.copy(0.18f) else Color.Transparent).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(line.text, color = if (isActive) Text1 else Text3, fontSize = fontSize.sp, fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal)
                if (isActive && line.translation != null) {
                    Text(line.translation, color = Accent2, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms < 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
