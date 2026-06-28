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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranik.app.data.model.Track
import com.tranik.app.ui.theme.*
import com.tranik.app.ui.viewmodel.TranslateViewModel

@Composable
fun TranslateScreen(
    vm: TranslateViewModel,
    currentTrack: Track?,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()
    val clipboardManager = LocalClipboardManager.current

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
            Text("ترجمه AI", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Text1)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.AutoAwesome, contentDescription = "هوش مصنوعی", tint = Accent, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Track info
        currentTrack?.let { track ->
            Text("آهنگ: ${track.displayName}", fontSize = 12.sp, color = Text4)
            Spacer(Modifier.height(8.dp))
        }

        // Language Selector
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.sourceLang == "en",
                onClick = { vm.setSourceLang("en") },
                label = { Text("EN") }
            )
            FilterChip(
                selected = state.sourceLang == "fa",
                onClick = { vm.setSourceLang("fa") },
                label = { Text("FA") }
            )

            IconButton(onClick = { vm.swapLanguages() }) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "جابجایی زبان", tint = Accent)
            }

            FilterChip(
                selected = state.targetLang == "fa",
                onClick = { vm.setTargetLang("fa") },
                label = { Text("FA") }
            )
            FilterChip(
                selected = state.targetLang == "en",
                onClick = { vm.setTargetLang("en") },
                label = { Text("EN") }
            )
        }

        Spacer(Modifier.height(12.dp))

        // Input
        OutlinedTextField(
            value = state.inputText,
            onValueChange = { vm.setInputText(it) },
            label = { Text("متن برای ترجمه") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Text1,
                focusedBorderColor = Accent,
                unfocusedBorderColor = DarkBg3,
                cursorColor = Accent
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        // Translate Button
        Button(
            onClick = { vm.translate() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(12.dp),
            enabled = !state.isLoading && state.inputText.isNotBlank()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Translate, null)
                Spacer(Modifier.width(8.dp))
                Text("ترجمه کن", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Error
        state.error?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Red.copy(0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = "خطا", tint = Red, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = Red, fontSize = 13.sp)
                }
            }
        }

        // Result
        state.result?.let { translated ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBg2),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("ترجمه:", fontSize = 11.sp, color = Text4)
                    Spacer(Modifier.height(6.dp))
                    Text(translated, color = Text1, fontSize = 15.sp, lineHeight = 22.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { clipboardManager.setText(AnnotatedString(translated)) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "کپی", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("کپی", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // History
        if (state.history.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("تاریخچه", fontSize = 13.sp, color = Text2, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            state.history.take(5).forEach { entry ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkBg3),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text(entry.sourceText.take(40), fontSize = 11.sp, color = Text3, maxLines = 1)
                        Text(entry.translatedText.take(40), fontSize = 11.sp, color = Accent2, maxLines = 1)
                    }
                }
            }
        }
    }
}
