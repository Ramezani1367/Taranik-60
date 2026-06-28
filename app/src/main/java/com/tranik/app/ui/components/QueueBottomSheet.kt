package com.tranik.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.tranik.app.data.model.Track
import com.tranik.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queue: List<Track>,
    currentIndex: Int,
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: String,
    onTrackClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkBg2,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {

            // Header
            Row(
                Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("صف پخش", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Text1)
                Spacer(Modifier.weight(1f))
                Text("${queue.size} آهنگ", fontSize = 12.sp, color = Text4)
                if (shuffleEnabled) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Shuffle, null, tint = Accent, modifier = Modifier.size(16.dp))
                }
            }

            Divider(color = DarkBg3, thickness = 1.dp)

            // Queue List
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                itemsIndexed(queue) { index, track ->
                    val isCurrent = index == currentIndex
                    val context = LocalContext.current

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrent) Accent.copy(0.12f) else Color.Transparent)
                            .clickable { onTrackClick(index) }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Index or Playing indicator
                        Box(
                            Modifier.size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrent) {
                                if (isPlaying) {
                                    Icon(Icons.Default.VolumeUp, null, tint = Accent, modifier = Modifier.size(18.dp))
                                } else {
                                    Icon(Icons.Default.Pause, null, tint = Accent, modifier = Modifier.size(18.dp))
                                }
                            } else {
                                Text(
                                    "${index + 1}",
                                    fontSize = 12.sp, color = Text4
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        // Cover
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (track.albumId >= 0) {
                                val albumArtUri = android.content.ContentUris.withAppendedId(
                                    android.net.Uri.parse("content://media/external/audio/albumart"),
                                    track.albumId
                                )
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(albumArtUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(Modifier.fillMaxSize().background(DarkBg3), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MusicNote, null, tint = Accent.copy(0.5f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                track.displayName,
                                fontSize = 14.sp,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isCurrent) Accent2 else Text1,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                track.displayArtist,
                                fontSize = 12.sp, color = Text3,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(track.duration, fontSize = 12.sp, color = Text4)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
