package com.tranik.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    private val _globalError = MutableStateFlow<String?>(null)
    val globalError = _globalError.asStateFlow()

    protected val errorHandler = CoroutineExceptionHandler { _, exception ->
        val message = when (exception) {
            is java.io.IOException -> "خطا در اتصال اینترنت. لطفاً اتصال خود را بررسی کنید."
            is java.net.SocketTimeoutException -> "سرور پاسخ نمی‌دهد. لطفاً دوباره تلاش کنید."
            is SecurityException -> "اجازه دسترسی داده نشده. لطفاً تنظیمات اپ را بررسی کنید."
            is IllegalArgumentException -> "درخواست نامعتبر: ${exception.message}"
            is NoSuchElementException -> "داده‌ای یافت نشد."
            is kotlinx.coroutines.CancellationException -> return@CoroutineExceptionHandler
            else -> "خطای غیرمنتظره: ${exception.message ?: "نامشخص"}"
        }
        _globalError.value = message
    }

    protected fun safeLaunch(block: suspend () -> Unit) {
        viewModelScope.launch(errorHandler) {
            try {
                block()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _globalError.value = e.message ?: "خطای نامشخص"
            }
        }
    }

    open fun clearError() {
        _globalError.value = null
    }
}
