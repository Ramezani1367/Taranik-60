package com.tranik.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranik.app.data.model.Track
import com.tranik.app.ui.theme.*
import com.tranik.app.ui.viewmodel.ExportViewModel

data class ExportFormatOption(
    val name: String,
    val extension: String,
    val description: String
)

@Composable
fun ExportScreen(
    vm: ExportViewModel,
    currentTrack: Track?,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val formats = listOf(
        ExportFormatOption("SRT", "srt", "SubRip — سازگار با اکثر پلیرها"),
        ExportFormatOption("LRC", "lrc", "Lyrics — فرمت استاندارد لیریک"),
        ExportFormatOption("ASS", "ass", "Advanced SubStation — با استایل"),
        ExportFormatOption("VTT", "vtt", "WebVTT — برای وب"),
        ExportFormatOption("TXT", "txt", "متن ساده — بدون زمان‌بندی")
    )

    // لود ترک
    LaunchedEffect(currentTrack) {
        currentTrack?.let { vm.loadTrack(it) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(18.dp)
    ) {

        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = Text2)
            }
            Text("خروجی زیرنویس", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Text1)
        }

        Spacer(Modifier.height(12.dp))

        // Track Info
        currentTrack?.let { track ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBg2),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, contentDescription = "آهنگ", tint = Accent)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(track.displayName, fontSize = 13.sp, color = Text1)
                        Text(
                            "${state.lines.size} خط • ${state.lines.count { it.synced }} زمان‌بندی شده",
                            fontSize = 11.sp, color = Text4
                        )
                    }
                }
            }
        } ?: Text("آهنگی انتخاب نشده", color = Text4, fontSize = 13.sp)

        Spacer(Modifier.height(16.dp))

        // Format Selection
        Text("فرمت خروجی", fontSize = 13.sp, color = Text2, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        formats.forEach { format ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { vm.setFormat(format.extension) }
                    .background(if (state.selectedFormat == format.extension) Accent.copy(0.15f) else Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = state.selectedFormat == format.extension,
                    onClick = { vm.setFormat(format.extension) },
                    colors = RadioButtonDefaults.colors(selectedColor = Accent)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(format.name, fontSize = 14.sp, color = Text1, fontWeight = FontWeight.SemiBold)
                    Text(format.description, fontSize = 11.sp, color = Text4)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Preview
        if (state.preview.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBg2),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Preview, contentDescription = "پیش‌نمایش", tint = Text4, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("پیش‌نمایش", fontSize = 12.sp, color = Text4)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.preview,
                        fontSize = 11.sp, color = Text2,
                        fontFamily = FontFamily.Monospace, lineHeight = 16.sp
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        // Export Button
        Button(
            onClick = { vm.exportFile(context) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(12.dp),
            enabled = !state.isExporting && state.lines.isNotEmpty() && state.lines.any { it.synced }
        ) {
            if (state.isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Download, null)
                Spacer(Modifier.width(8.dp))
                Text("دانلود فایل ${state.selectedFormat.uppercase()}", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Share / Copy
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { vm.shareFile(context) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "اشتراک", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("اشتراک", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = {
                    if (state.preview.isNotBlank())
                        clipboardManager.setText(AnnotatedString(state.preview))
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "کپی", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("کپی", fontSize = 13.sp)
            }
        }

        // Success / Error
        if (state.exportSuccess) {
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Green.copy(0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "موفق", tint = Green, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("فایل ذخیره شد در Downloads ✓", color = Green, fontSize = 13.sp)
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
