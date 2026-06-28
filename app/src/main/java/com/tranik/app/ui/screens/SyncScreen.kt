package com.tranik.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranik.app.data.model.LyricLine
import com.tranik.app.data.model.Track
import com.tranik.app.data.model.formatDuration
import com.tranik.app.ui.theme.*
import com.tranik.app.ui.viewmodel.PlayerViewModel
import com.tranik.app.ui.viewmodel.SyncViewModel
import com.tranik.app.ui.viewmodel.LyricsViewModel

@Composable
fun SyncScreen(
    vm: SyncViewModel,
    playerVm: PlayerViewModel,
    lyricsVm: LyricsViewModel,
    onBack: () -> Unit
) {
    val syncState by vm.state.collectAsState()
    val playerState by playerVm.state.collectAsState()
    val lyricsState by lyricsVm.state.collectAsState()
    val track = playerState.currentTrack

    // لود لیریک واقعی از ViewModel
    LaunchedEffect(track) {
        if (syncState.lines.isEmpty() && track != null) {
            lyricsVm.loadTrack(track)
        }
    }

    // وقتی لیریک لود شد، خطوط رو به SyncViewModel بفرست
    LaunchedEffect(lyricsState.lyricsText) {
        if (syncState.lines.isEmpty() && lyricsState.lyricsText.isNotBlank()) {
            vm.loadLyrics(lyricsState.lyricsText)
        }
    }

    Column(Modifier.fillMaxSize().background(DarkBg)) {

        // Header
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = Text2)
            }
            Text("همگام‌سازی", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Text1)
            Spacer(Modifier.weight(1f))
            Text(syncState.progressText, fontSize = 12.sp, color = Text4)
        }

        // Mini Player Bar
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBg2),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerVm.togglePlay() }) {
                    Icon(
                        if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null, tint = Text1
                    )
                }
                Text(
                    formatDuration(playerState.currentPositionMs),
                    fontSize = 13.sp, color = Accent, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    track?.displayName ?: "",
                    fontSize = 12.sp, color = Text3,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Lines
        if (syncState.lines.isEmpty()) {
            Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.TextFields, contentDescription = "متن", tint = Text4, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("لیریکی برای همگام‌سازی وجود ندارد", color = Text4, fontSize = 14.sp)
                    Text("ابتدا لیریک را وارد کنید", color = Text4, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).padding(horizontal = 18.dp)) {
                itemsIndexed(syncState.lines) { i, line ->
                    SyncLineItem(
                        line = line,
                        isCurrent = i == syncState.currentIndex,
                        onStamp = { vm.stampLine(playerState.currentPositionMs) },
                        onAdjust = { delta -> vm.adjustTime(i, delta) }
                    )
                }
            }
        }

        // Bottom Controls
        Column(Modifier.padding(18.dp)) {
            Button(
                onClick = { vm.stampLine(playerState.currentPositionMs) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(14.dp),
                enabled = !syncState.isComplete && syncState.lines.isNotEmpty()
            ) {
                Icon(Icons.Default.Timer, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (syncState.isComplete) "تکمیل شد ✓" else "ثبت زمان",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { vm.undoLastStamp() },
                    enabled = syncState.canUndo,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "برگشت", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("برگشت")
                }
                OutlinedButton(
                    onClick = { vm.resetAllStamps() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "شروع مجدد", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("شروع مجدد")
                }
            }

            // Export LRC
            if (syncState.isComplete && track != null) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { vm.exportLrc(track.filePath) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileDownload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("خروجی LRC", fontWeight = FontWeight.Bold)
                }
            }

            // Save result
            syncState.saveResult?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Green.copy(0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "موفق", tint = Green, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(msg, color = Green, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncLineItem(
    line: LyricLine,
    isCurrent: Boolean,
    onStamp: () -> Unit,
    onAdjust: (Long) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrent) Accent.copy(0.15f) else Color.Transparent)
            .clickable { onStamp() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time
        Text(
            line.time,
            fontSize = 13.sp,
            color = if (line.synced) Green else if (isCurrent) Accent else Text4,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(60.dp)
        )

        // Fine-tune buttons
        if (line.synced) {
            IconButton(onClick = { onAdjust(-100) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.FastRewind, contentDescription = "عقب", tint = Text4, modifier = Modifier.size(14.dp))
            }
            IconButton(onClick = { onAdjust(100) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.FastForward, contentDescription = "جلو", tint = Text4, modifier = Modifier.size(14.dp))
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }

        // Text
        Text(
            line.text,
            fontSize = 14.sp,
            color = if (isCurrent) Text1 else Text3,
            modifier = Modifier.weight(1f)
        )

        // Status icon
        if (line.synced) {
            Icon(Icons.Default.CheckCircle, contentDescription = "موفق", tint = Green, modifier = Modifier.size(18.dp))
        } else if (isCurrent) {
            Icon(Icons.Default.PlayArrow, contentDescription = "فعلی", tint = Accent, modifier = Modifier.size(18.dp))
        }
    }
}
