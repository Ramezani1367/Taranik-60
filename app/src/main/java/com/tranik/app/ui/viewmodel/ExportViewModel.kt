package com.tranik.app.ui.viewmodel

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.tranik.app.ui.viewmodel.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.tranik.app.data.model.LyricLine
import com.tranik.app.data.model.Track
import com.tranik.app.data.repository.LyricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import javax.inject.Inject

data class ExportState(
    val track: Track? = null,
    val lines: List<LyricLine> = emptyList(),
    val selectedFormat: String = "srt",
    val preview: String = "",
    val isExporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val lyricsRepository: LyricsRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(ExportState())
    val state = _state.asStateFlow()

    fun loadTrack(track: Track) {
        _state.update { it.copy(track = track, exportSuccess = false, error = null) }

        viewModelScope.launch {
            // لود لیریک
            val lrcPath = lyricsRepository.findLrcForTrack(track.filePath)
            val lines = if (lrcPath != null) {
                lyricsRepository.readLrcFile(lrcPath)
            } else emptyList()

            _state.update { it.copy(lines = lines) }
            generatePreview()
        }
    }

    fun setFormat(format: String) {
        _state.update { it.copy(selectedFormat = format) }
        generatePreview()
    }

    private fun generatePreview() {
        val lines = _state.value.lines
        val format = _state.value.selectedFormat

        val preview = when (format) {
            "srt" -> generateSrt(lines)
            "lrc" -> generateLrc(lines)
            "ass" -> generateAss(lines)
            "vtt" -> generateVtt(lines)
            "txt" -> lines.joinToString("\n") { it.text }
            else -> ""
        }

        _state.update { it.copy(preview = preview.take(800)) }
    }

    fun exportFile(context: Context) {
        val track = _state.value.track ?: return
        val format = _state.value.selectedFormat
        val content = when (format) {
            "srt" -> generateSrt(_state.value.lines)
            "lrc" -> generateLrc(_state.value.lines)
            "ass" -> generateAss(_state.value.lines)
            "vtt" -> generateVtt(_state.value.lines)
            "txt" -> _state.value.lines.joinToString("\n") { it.text }
            else -> return
        }

        val fileName = "${track.displayName}.${format}"

        _state.update { it.copy(isExporting = true, exportSuccess = false, error = null) }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    saveToDownloads(context, fileName, content, format)
                }
                _state.update { it.copy(isExporting = false, exportSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isExporting = false, error = e.message) }
            }
        }
    }

    private fun saveToDownloads(context: Context, fileName: String, content: String, format: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ — MediaStore
            val mimeType = when (format) {
                "srt" -> "application/x-subrip"
                "ass" -> "text/plain"
                "vtt" -> "text/vtt"
                else -> "text/plain"
            }

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    OutputStreamWriter(stream).use { writer ->
                        writer.write(content)
                    }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
        } else {
            // Android 9 و قدیمی‌تر — مستقیم
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(dir, fileName)
            file.writeText(content)
        }
    }

    fun shareFile(context: Context) {
        val content = _state.value.preview
        if (content.isBlank()) return

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
    }

    // ==================== فرمت‌سازها ====================

    private fun generateSrt(lines: List<LyricLine>): String {
        val syncedLines = lines.filter { it.synced && it.timeMs >= 0 }
        return syncedLines.mapIndexed { i, line ->
            val startMs = line.timeMs
            val endMs = syncedLines.getOrNull(i + 1)?.timeMs ?: (startMs + 5000)
            "${i + 1}\n${formatSrtTime(startMs)} --> ${formatSrtTime(endMs)}\n${line.text}"
        }.joinToString("\n\n")
    }

    private fun generateLrc(lines: List<LyricLine>): String {
        return lines
            .filter { it.synced && it.timeMs >= 0 }
            .joinToString("\n") { "[${it.lrcTimeTag}]${it.text}" }
    }

    private fun generateAss(lines: List<LyricLine>): String {
        val header = """[Script Info]
Title: TarAnik Export
ScriptType: v4.00+
PlayResX: 384
PlayResY: 288

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,-1,0,0,0,100,100,0,0,1,2,1,2,10,10,10,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
"""
        val events = lines
            .filter { it.synced && it.timeMs >= 0 }
            .mapIndexed { i, line ->
                val start = formatAssTime(line.timeMs)
                val end = lines.getOrNull(i + 1)?.let { formatAssTime(it.timeMs) } ?: formatAssTime(line.timeMs + 5000)
                "Dialogue: 0,$start,$end,Default,,0,0,0,,${line.text}"
            }
            .joinToString("\n")

        return header + events
    }

    private fun generateVtt(lines: List<LyricLine>): String {
        val header = "WEBVTT\n\n"
        val events = lines
            .filter { it.synced && it.timeMs >= 0 }
            .mapIndexed { i, line ->
                val start = formatVttTime(line.timeMs)
                val end = lines.getOrNull(i + 1)?.let { formatVttTime(it.timeMs) } ?: formatVttTime(line.timeMs + 5000)
                "$start --> $end\n${line.text}"
            }
            .joinToString("\n\n")

        return header + events
    }

    private fun formatSrtTime(ms: Long): String {
        val h = ms / 3600000
        val m = (ms % 3600000) / 60000
        val s = (ms % 60000) / 1000
        val mil = ms % 1000
        return "%02d:%02d:%02d,%03d".format(h, m, s, mil)
    }

    private fun formatAssTime(ms: Long): String {
        val h = ms / 3600000
        val m = (ms % 3600000) / 60000
        val s = (ms % 60000) / 1000
        val cs = (ms % 1000) / 10
        return "%d:%02d:%02d.%02d".format(h, m, s, cs)
    }

    private fun formatVttTime(ms: Long): String {
        val h = ms / 3600000
        val m = (ms % 3600000) / 60000
        val s = (ms % 60000) / 1000
        val mil = ms % 1000
        return "%02d:%02d:%02d.%03d".format(h, m, s, mil)
    }
}
