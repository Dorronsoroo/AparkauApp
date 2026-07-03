package com.lksnext.ParkingJDorronsoro.common.snackbar

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SnackbarManager {

    private val _messages: MutableStateFlow<SnackbarMessage?> = MutableStateFlow(null)
    val snackbarMessages: StateFlow<SnackbarMessage?> get() = _messages.asStateFlow()

    fun showMessage(@StringRes message: Int) {
        _messages.value = SnackbarMessage.ResourceSnackbar(message)
    }

    fun showMessage(message: SnackbarMessage) {
        _messages.value = message
    }

    fun clearSnackbarState() {
        _messages.value = null
    }
}

