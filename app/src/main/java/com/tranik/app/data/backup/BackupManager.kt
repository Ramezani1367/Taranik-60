package com.tranik.app.data.backup

import android.content.Context
import com.tranik.app.data.repository.AppSettings
import com.tranik.app.data.repository.SettingsRepository
import com.tranik.app.data.model.Folder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدیریت بکاپ و ریستور تنظیمات و اطلاعات اپ
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    /**
     * ساخت فایل بکاپ JSON
     */
    suspend fun createBackup(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val settings = settingsRepository.settings.let { flow ->
                // گرفتن آخرین مقدار از Flow
                var result: AppSettings? = null
                flow.collect { result = it }
                result ?: AppSettings()
            }

            val json = JSONObject().apply {
                put("version", 1)
                put("app_version", "1.2.0")
                put("timestamp", System.currentTimeMillis())
                put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

                put("settings", JSONObject().apply {
                    put("theme", settings.theme)
                    put("language", settings.language)
                    put("autoSync", settings.autoSync)
                    put("downloadCovers", settings.downloadCovers)
                    put("playbackSpeed", settings.playbackSpeed)
                })

                put("scanFolders", JSONArray().apply {
                    settings.scanFolders.forEach { folder ->
                        put(folder)
                    }
                })
            }

            // ذخیره در فایل
            val backupDir = File(context.getExternalFilesDir(null), "backups")
            backupDir.mkdirs()
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "taranik_backup_$dateStr.json")
            backupFile.writeText(json.toString(2))

            Result.success(backupFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ریستور از فایل بکاپ
     */
    suspend fun restoreBackup(backupPath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(backupPath)
            if (!file.exists()) return@withContext Result.failure(Exception("فایل بکاپ یافت نشد"))

            val json = JSONObject(file.readText())
            val settingsJson = json.optJSONObject("settings") ?: return@withContext Result.failure(Exception("فرمت بکاپ نامعتبر"))

            // ریستور تنظیمات
            settingsJson.optString("theme").takeIf { it.isNotEmpty() }?.let { settingsRepository.saveTheme(it) }
            settingsJson.optString("language").takeIf { it.isNotEmpty() }?.let { settingsRepository.saveLanguage(it) }
            settingsJson.optBoolean("autoSync", true).let { settingsRepository.saveAutoSync(it) }
            settingsJson.optBoolean("downloadCovers", true).let { settingsRepository.saveDownloadCovers(it) }
            settingsJson.optDouble("playbackSpeed", 1.0).let { settingsRepository.savePlaybackSpeed(it.toFloat()) }

            // ریستور فولدرهای اسکن
            val foldersArray = json.optJSONArray("scanFolders")
            if (foldersArray != null) {
                val folders = mutableSetOf<String>()
                for (i in 0 until foldersArray.length()) {
                    folders.add(foldersArray.getString(i))
                }
                settingsRepository.saveScanFolders(folders)
            }

            val backupDate = json.optString("date", "نامشخص")
            Result.success("تنظیمات بازیابی شد از $backupDate")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * لیست فایل‌های بکاپ
     */
    fun getBackupFiles(): List<File> {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) return emptyList()
        return backupDir.listFiles()
            ?.filter { it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * حذف فایل بکاپ
     */
    fun deleteBackup(file: File): Boolean {
        return file.delete()
    }
}
