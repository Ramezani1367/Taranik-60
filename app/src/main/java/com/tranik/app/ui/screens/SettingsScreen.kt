package com.tranik.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranik.app.ui.theme.*
import com.tranik.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }
    var showBugReportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 18.dp)
    ) {

        // Header
        item {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = Text2)
                }
                Text("تنظیمات", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Text1)
            }
            Spacer(Modifier.height(20.dp))
        }

        // Version Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBg2),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = "آیکون اپ", tint = Accent, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("ترانیک", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Text1)
                        Text("نسخه ${state.versionName}", fontSize = 12.sp, color = Text4)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // === ظاهر ===
        item { SectionHeader("ظاهر") }

        item {
            Text("تم", fontSize = 13.sp, color = Text2)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("dark" to "🌙 تاریک", "light" to "☀️ روشن", "system" to "🔄 سیستم").forEach { (key, label) ->
                    FilterChip(
                        selected = state.theme == key,
                        onClick = { vm.setTheme(key) },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // === کتابخانه ===
        item { SectionHeader("کتابخانه") }

        item {
            SettingsItem(
                icon = Icons.Default.DeleteSweep,
                title = "پاک‌سازی کش",
                subtitle = "حجم: ${state.cacheSize}",
                onClick = { vm.clearCache(context) }
            )
            Spacer(Modifier.height(4.dp))
        }

        item {
            SettingsSwitch(
                icon = Icons.Default.Sync,
                title = "همگام‌سازی خودکار",
                checked = state.autoSync,
                onCheckedChange = { vm.setAutoSync(it) }
            )
            Spacer(Modifier.height(4.dp))
        }

        item {
            SettingsSwitch(
                icon = Icons.Default.Image,
                title = "دانلود خودکار کاور",
                checked = state.downloadCovers,
                onCheckedChange = { vm.setDownloadCovers(it) }
            )
            Spacer(Modifier.height(16.dp))
        }

        // === درباره ===
        item { SectionHeader("درباره") }

        item {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "درباره ترانیک",
                subtitle = "نسخه ${state.versionName} • ساخته شده با ❤️",
                onClick = { showAboutDialog = true }
            )
            Spacer(Modifier.height(4.dp))
        }

        item {
            SettingsItem(
                icon = Icons.Default.BugReport,
                title = "گزارش باگ",
                subtitle = "ارسال بازخورد",
                onClick = { showBugReportDialog = true }
            )
            Spacer(Modifier.height(4.dp))
        }

        item {
            SettingsItem(
                icon = Icons.Default.Star,
                title = "امتیازدهی",
                subtitle = "در کافه بازار امتیاز بده",
                onClick = { openStorePage(context) }
            )
            Spacer(Modifier.height(4.dp))
        }

        item {
            SettingsItem(
                icon = Icons.Default.Share,
                title = "اشتراک‌گذاری",
                subtitle = "معرفی ترانیک به دوستان",
                onClick = { shareApp(context) }
            )
        }
    }

    // ✅ About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("درباره ترانیک") },
            text = {
                Column {
                    Text("ترانیک نسخه ${state.versionName}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("پلیر موسیقی فارسی با قابلیت ویرایش تگ، همگام‌سازی لیریک، ترجمه AI و خروجی زیرنویس.")
                    Spacer(Modifier.height(8.dp))
                    Text("ساخته شده با ❤️ در ایران", color = Text3, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("تکنولوژی: Kotlin, Jetpack Compose, Hilt, MediaStore", color = Text4, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("بستن") }
            }
        )
    }

    // ✅ Bug Report Dialog
    if (showBugReportDialog) {
        AlertDialog(
            onDismissRequest = { showBugReportDialog = false },
            title = { Text("گزارش باگ") },
            text = {
                Column {
                    Text("لطفاً باگ رو با جزئیات توضیح بده:")
                    Spacer(Modifier.height(8.dp))
                    Text("• چه مرحله‌ای اتفاق افتاد؟", fontSize = 13.sp)
                    Text("• چه خطایی دیدی؟", fontSize = 13.sp)
                    Text("• مدل گوشی و نسخه اندروید", fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("از طریق ایمیل ارسال میشه:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("taranik.app@gmail.com", color = Accent, fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    sendBugReportEmail(context)
                    showBugReportDialog = false
                }) { Text("ارسال ایمیل") }
            },
            dismissButton = {
                TextButton(onClick = { showBugReportDialog = false }) { Text("انصراف") }
            }
        )
    }
}

private fun openStorePage(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.tranik.app"))
        context.startActivity(intent)
    } catch (e: Exception) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.tranik.app"))
        context.startActivity(intent)
    }
}

private fun shareApp(context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "ترانیک - پلیر موسیقی فارسی")
        putExtra(Intent.EXTRA_TEXT, "ترانیک رو امتحان کن! پلیر موسیقی با ویرایش تگ و لیریک و ترجمه AI\nhttps://play.google.com/store/apps/details?id=com.tranik.app")
    }
    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری ترانیک"))
}

private fun sendBugReportEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:taranik.app@gmail.com")
        putExtra(Intent.EXTRA_SUBJECT, "گزارش باگ - ترانیک")
        putExtra(Intent.EXTRA_TEXT, "سلام، یه باگ پیدا کردم:\n\nمراحل:\n\nخطا:\n\nمدل گوشی:\nنسخه اندروید:\nنسخه اپ: 1.2.0")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // اپ ایمیل نصب نیست
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, fontSize = 12.sp, color = Text4, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onClick() }.padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = Text2, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = Text1)
            Text(subtitle, fontSize = 11.sp, color = Text4)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "بیشتر", tint = Text4, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsSwitch(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = title, tint = Text2, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 14.sp, color = Text1, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = Accent))
    }
}
