package com.tranik.app.ui.viewmodel

import com.tranik.app.ui.viewmodel.BaseViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import javax.inject.Inject

data class TranslateState(
    val inputText: String = "",
    val result: String? = null,
    val sourceLang: String = "en",
    val targetLang: String = "fa",
    val isLoading: Boolean = false,
    val error: String? = null,
    val history: List<TranslationEntry> = emptyList()
)

data class TranslationEntry(
    val sourceText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class TranslateViewModel @Inject constructor() : BaseViewModel() {
    private val _state = MutableStateFlow(TranslateState())
    val state = _state.asStateFlow()

    fun setInputText(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun setSourceLang(lang: String) {
        _state.update { it.copy(sourceLang = lang) }
    }

    fun setTargetLang(lang: String) {
        _state.update { it.copy(targetLang = lang) }
    }

    fun swapLanguages() {
        _state.update {
            it.copy(sourceLang = it.targetLang, targetLang = it.sourceLang, result = null, inputText = it.result ?: it.inputText)
        }
    }

    fun translate() {
        val text = _state.value.inputText
        if (text.isBlank()) return
        _state.update { it.copy(isLoading = true, error = null, result = null) }

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = "https://api.mymemory.translated.net/get?q=" + URLEncoder.encode(text, "UTF-8") + "&langpair=${_state.value.sourceLang}|${_state.value.targetLang}"
                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().readText()
                        parseTranslationResponse(response)
                    } else {
                        throw Exception("خطای سرور: $responseCode")
                    }
                }
                _state.update {
                    it.copy(isLoading = false, result = result, history = listOf(TranslationEntry(sourceText = text, translatedText = result, sourceLang = it.sourceLang, targetLang = it.targetLang)) + it.history.take(19))
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "خطا در ترجمه. اتصال اینترنت را بررسی کنید.") }
            }
        }
    }

    private fun parseTranslationResponse(json: String): String {
        val responseDataKey = "\"responseData\":"
        val translatedTextKey = "\"translatedText\":"
        val responseDataIndex = json.indexOf(responseDataKey)
        if (responseDataIndex < 0) throw Exception("پاسخ نامعتبر")
        val textIndex = json.indexOf(translatedTextKey, responseDataIndex)
        if (textIndex < 0) throw Exception("پاسخ نامعتبر")
        val startQuote = json.indexOf("\"", textIndex + translatedTextKey.length)
        val endQuote = json.indexOf("\"", startQuote + 1)
        if (startQuote < 0 || endQuote < 0) throw Exception("پاسخ نامعتبر")
        return json.substring(startQuote + 1, endQuote).replace("\\n", "\n").replace("\\\"", "\"").replace("\\u0026", "&")
    }

    override fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
