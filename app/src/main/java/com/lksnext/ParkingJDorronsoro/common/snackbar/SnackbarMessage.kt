package com.lksnext.ParkingJDorronsoro.common.snackbar

import androidx.annotation.StringRes

sealed class SnackbarMessage {
    class StringSnackbar(val message: String) : SnackbarMessage()
    class ResourceSnackbar(@StringRes val message: Int) : SnackbarMessage()

    companion object {
        fun Throwable.toSnackbarMessage(): SnackbarMessage {
            val message = this.message.orEmpty()
            return if (message.isNotBlank()) StringSnackbar(message)
            else StringSnackbar("Ha ocurrido un error desconocido.")
        }
    }
}

