package com.tranik.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.tranik.app.data.model.Track
import com.tranik.app.ui.theme.*
import com.tranik.app.ui.viewmodel.LyricsViewModel

@Composable
fun LyricsEditorScreen(
    vm: LyricsViewModel,
    onNavigateToSync: () -> Unit,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().background(DarkBg).padding(18.dp)) {

        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = Text2)
            }
            Text("ویرایش لایریک", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Text1)
            Spacer(Modifier.weight(1f))
            state.track?.let {
                Text(it.displayName, fontSize = 11.sp, color = Text4, maxLines = 1)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Format Toggle
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !state.isLrcFormat,
                onClick = { vm.setLrcFormat(false) },
                label = { Text("متن ساده") }
            )
            FilterChip(
                selected = state.isLrcFormat,
                onClick = { vm.setLrcFormat(true) },
                label = { Text("LRC") }
            )
        }

        Spacer(Modifier.height(12.dp))

        // Toolbar
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { vm.clearLyrics() }) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "پاک کردن", tint = Text4)
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${state.lyricsText.lines().size} خط",
                fontSize = 11.sp, color = Text4,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Text Editor
        OutlinedTextField(
            value = state.lyricsText,
            onValueChange = { vm.updateLyricsText(it) },
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Text1,
                unfocusedTextColor = Text2,
                focusedBorderColor = Accent,
                unfocusedBorderColor = DarkBg3,
                cursorColor = Accent
            ),
            placeholder = {
                Text(
                    if (state.isLrcFormat) "[00:12.00]خط لیریک\n[00:18.50]خط بعدی"
                    else "لایریک را اینجا وارد کنید…",
                    color = Text4
                )
            },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(20.dp))

        // Save Button
        Button(
            onClick = { vm.saveLyrics() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(12.dp),
            enabled = !state.isSaving && state.lyricsText.isNotBlank() && state.track != null
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("ذخیره لایریک", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Navigate to Sync
        OutlinedButton(
            onClick = onNavigateToSync,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = state.lyricsText.isNotBlank()
        ) {
            Icon(Icons.Default.Sync, contentDescription = "همگام‌سازی", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("همگام‌سازی زمان‌بندی")
        }

        // Save result
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
}
