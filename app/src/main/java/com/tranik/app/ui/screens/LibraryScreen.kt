package com.tranik.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tranik.app.data.model.Folder
import com.tranik.app.data.model.Track
import com.tranik.app.ui.theme.*
import com.tranik.app.ui.viewmodel.LibraryViewModel
import com.tranik.app.ui.viewmodel.PlayerViewModel

@Composable
fun LibraryScreen(
    vm: LibraryViewModel,
    playerVm: PlayerViewModel,
    onPlay: (Track, Context) -> Unit,
    onEdit: (Track) -> Unit
) {
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().background(DarkBg)) {

        // Header
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "ترانیک",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.linearGradient(listOf(Accent, Accent2, Pink))
                ),
                color = Color.Transparent
            )
            Spacer(Modifier.weight(1f))
            Text("${state.filteredTracks.size} آهنگ", fontSize = 11.sp, color = Text4)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { vm.refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = "بازنشانی", tint = Text4)
            }
        }

        // Search
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { vm.search(it) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            placeholder = { Text("جستجو در آهنگ‌ها…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "جستجو", tint = Text4) },
            trailingIcon = {
                if (state.searchQuery.isNotBlank()) {
                    IconButton(onClick = { vm.search("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "پاک کردن", tint = Text4)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DarkBg3,
                unfocusedBorderColor = DarkBg3,
                focusedTextColor = Text1,
                cursorColor = Accent
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        // Folders
        if (state.folders.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.folders) { folder ->
                    val active = folder.path == state.activeFolderPath
                    Surface(
                        onClick = { vm.selectFolder(folder.path) },
                        shape = RoundedCornerShape(22.dp),
                        color = if (active) Accent.copy(0.18f) else DarkBg2,
                        border = BorderStroke(1.dp, if (active) Accent else Color(0xFF282836))
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder, contentDescription = "پوشه",
                                tint = if (active) Accent2 else Text3,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(folder.name, fontSize = 12.5.sp, color = if (active) Accent2 else Text3)
                            Spacer(Modifier.width(4.dp))
                            Text("(${folder.trackCount})", fontSize = 10.sp, color = Text4)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Info
        val activeFolder = state.folders.find { it.path == state.activeFolderPath }
        Text(
            "${activeFolder?.name ?: "همه"} • ${state.filteredTracks.size} آهنگ",
            fontSize = 11.sp, color = Text4,
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Content
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            }

            state.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, contentDescription = "خطا", tint = Red, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(state.error ?: "خطا", color = Red, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.refresh() }) { Text("تلاش مجدد") }
                    }
                }
            }

            state.filteredTracks.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LibraryMusic, contentDescription = "کتابخانه خالی", modifier = Modifier.size(64.dp), tint = Text4)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (state.searchQuery.isNotBlank()) "نتیجه‌ای یافت نشد"
                            else "آهنگی یافت نشد\nفولدری با فایل صوتی پیدا نشد",
                            color = Text4, fontSize = 14.sp, textAlign = TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.filteredTracks, key = { it.id }) { track ->
                        TrackItem(
                            track = track,
                            albumArtUri = if (track.albumId >= 0) playerVm.getAlbumArtUri(track.albumId) else null,
                            onPlay = { onPlay(track, it) },
                            onEdit = { onEdit(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackItem(
    track: Track,
    albumArtUri: android.net.Uri?,
    onPlay: (android.content.Context) -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onPlay(context) }
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover Art with Coil
        Box(
            Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (albumArtUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(albumArtUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "کاور",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier.fillMaxSize().background(DarkBg3),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote, contentDescription = "آهنگ",
                        tint = Accent.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                track.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Text1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${track.displayArtist} • ${track.duration}",
                fontSize = 12.sp, color = Text3,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row {
                Badge(text = if (track.hasLyric) "لایریک" else "بدون لیریک", active = track.hasLyric, activeColor = Pink)
                Spacer(Modifier.width(6.dp))
                Badge(text = if (track.tagClean) "تگ تمیز" else "کثیف", active = track.tagClean, activeColor = Green)
            }
        }

        IconButton(onClick = onEdit) {
            Icon(Icons.Default.MoreVert, contentDescription = "گزینه‌های بیشتر", tint = Text4)
        }
    }
}

@Composable
private fun Badge(text: String, active: Boolean, activeColor: Color) {
    Box(
        Modifier
            .background(
                if (active) activeColor.copy(alpha = 0.16f) else Color(0x22FFFFFF),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 9.sp, color = if (active) activeColor else Text4)
    }
}
