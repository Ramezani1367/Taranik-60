package com.tranik.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranik.app.data.model.DirtyTag
import com.tranik.app.ui.theme.*
import com.tranik.app.ui.viewmodel.TagEditorViewModel

@Composable
fun TagEditorScreen(
    vm: TagEditorViewModel,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()
    val track = state.track
    val tags = state.tagFields
    var showMoreFields by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {

        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = Text2)
            }
            Text("ویرایش تگ ID3", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Text1)
        }

        if (track == null) {
            Box(Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
                Text("ابتدا یک آهنگ انتخاب کنید", color = Text4, fontSize = 14.sp)
            }
            return
        }

        Spacer(Modifier.height(12.dp))

        // Track Info + Cover Art
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Cover Art
            val context = androidx.compose.ui.platform.LocalContext.current
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (track.albumId >= 0) {
                    val uri = com.tranik.app.data.repository.TrackRepository::class.java
                    // استفاده مستقیم از MediaStore URI
                    val albumArtUri = android.content.ContentUris.withAppendedId(
                        android.net.Uri.parse("content://media/external/audio/albumart"),
                        track.albumId
                    )
                    coil.compose.AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(albumArtUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "کاور",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(DarkBg3), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Album, contentDescription = "آلبوم", tint = Text4, modifier = Modifier.size(32.dp))
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(track.displayName, fontSize = 15.sp, color = Text1, fontWeight = FontWeight.SemiBold)
                Text(track.displayArtist, fontSize = 13.sp, color = Text3)
                Text(track.displayAlbum, fontSize = 12.sp, color = Text4)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Dirty Tags Warning
        if (state.dirtyTags.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Red.copy(0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "هشدار", tint = Red, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${state.dirtyTags.size} تگ مشکوک", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Red)
                    }
                    Spacer(Modifier.height(8.dp))
                    state.dirtyTags.forEach { tag ->
                        DirtyTagItem(
                            tag = tag,
                            onClean = { vm.cleanTag(tag.key) },
                            onIgnore = { vm.ignoreTag(tag.key) }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Main Fields
        TagField("عنوان", tags["title"] ?: "") { vm.updateField("title", it) }
        Spacer(Modifier.height(8.dp))
        TagField("هنرمند", tags["artist"] ?: "") { vm.updateField("artist", it) }
        Spacer(Modifier.height(8.dp))
        TagField("آلبوم", tags["album"] ?: "") { vm.updateField("album", it) }

        // More Fields
        if (showMoreFields) {
            Spacer(Modifier.height(8.dp))
            TagField("هنرمند آلبوم", tags["albumArtist"] ?: "") { vm.updateField("albumArtist", it) }
            Spacer(Modifier.height(8.dp))
            TagField("سال", tags["year"] ?: "") { vm.updateField("year", it) }
            Spacer(Modifier.height(8.dp))
            TagField("شماره ترک", tags["trackNumber"] ?: "") { vm.updateField("trackNumber", it) }
            Spacer(Modifier.height(8.dp))
            TagField("ژانر", tags["genre"] ?: "") { vm.updateField("genre", it) }
            Spacer(Modifier.height(8.dp))
            TagField("آهنگساز", tags["composer"] ?: "") { vm.updateField("composer", it) }
            Spacer(Modifier.height(8.dp))
            TagField("نظر", tags["comment"] ?: "") { vm.updateField("comment", it) }
        }

        TextButton(onClick = { showMoreFields = !showMoreFields }) {
            Icon(
                if (showMoreFields) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null, modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (showMoreFields) "نمایش کمتر" else "فیلدهای بیشتر",
                color = Accent2, fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        // Save Button
        Button(
            onClick = { showConfirmDialog = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(12.dp),
            enabled = !state.isSaving
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("ذخیره روی فایل اصلی", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("ذخیره مستقیم روی حافظه • ID3v2.4", fontSize = 11.sp, color = Text4)

        // Save Result
        state.saveResult?.let { msg ->
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

        state.error?.let { err ->
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Red.copy(0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = "خطا", tint = Red, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(err, color = Red, fontSize = 13.sp)
                }
            }
        }
    }

    // Confirm Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("تایید ذخیره‌سازی") },
            text = { Text("تغییرات مستقیماً روی فایل اصلی ذخیره می‌شود. ادامه؟") },
            confirmButton = {
                Button(
                    onClick = { vm.saveID3(); showConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) { Text("ذخیره") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) { Text("انصراف") }
            }
        )
    }
}

@Composable
private fun TagField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Text1,
            unfocusedTextColor = Text2,
            focusedBorderColor = Accent,
            unfocusedBorderColor = DarkBg3,
            cursorColor = Accent
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
private fun DirtyTagItem(
    tag: DirtyTag,
    onClean: () -> Unit,
    onIgnore: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkBg2),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(tag.field, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Text1)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = "تگ مشکوک", tint = Red, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(tag.oldValue, fontSize = 11.sp, color = Red)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = "پاک", tint = Green, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(tag.newValue, fontSize = 11.sp, color = Green)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onClean,
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) { Text("پاک‌سازی", fontSize = 11.sp) }
                OutlinedButton(
                    onClick = onIgnore,
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) { Text("نادیده", fontSize = 11.sp) }
            }
        }
    }
}
