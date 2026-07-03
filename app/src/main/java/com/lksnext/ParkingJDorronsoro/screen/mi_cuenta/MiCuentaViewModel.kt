package com.lksnext.ParkingJDorronsoro.screen.mi_cuenta

import androidx.compose.runtime.mutableStateOf
import com.lksnext.ParkingJDorronsoro.AparkauRoutes
import com.lksnext.ParkingJDorronsoro.MakeItSoViewModel
import com.lksnext.ParkingJDorronsoro.common.AppText
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarManager
import com.lksnext.ParkingJDorronsoro.model.service.AccountService
import com.lksnext.ParkingJDorronsoro.model.service.LogService
import com.lksnext.ParkingJDorronsoro.model.service.NotificacionService
import com.lksnext.ParkingJDorronsoro.model.service.UsuarioService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MiCuentaViewModel @Inject constructor(
    private val accountService: AccountService,
    private val usuarioService: UsuarioService,
    private val notificacionService: NotificacionService,
    logService: LogService
) : MakeItSoViewModel(logService) {

    var uiState = mutableStateOf(MiCuentaUiState())
        private set

    private val email get() = uiState.value.email
    private val perfil get() = uiState.value.perfil

    init {
        cargarUsuario()
    }

    fun onEmailChange(newValue: String) {
        uiState.value = uiState.value.copy(email = newValue)
    }


    fun onGuardarClick() {
        if (email.isBlank()) {
            SnackbarManager.showMessage(AppText.email_error)
            return
        }

        launchCatching {
            val resultado = usuarioService.actualizarUsuario(
                uid = accountService.currentUserId,
                email = email.trim(),
                perfil = perfil
            )
            resultado.getOrElse { error -> throw error }

            SnackbarManager.showMessage(AppText.datos_guardados)
        }
    }

    fun onMisCochesClick(openScreen: (String) -> Unit) {
        openScreen(AparkauRoutes.MIS_COCHES_SCREEN)
    }

    fun onVolverClick(openAndPopUp: (String, String) -> Unit) {
        openAndPopUp(AparkauRoutes.HOME_SCREEN, AparkauRoutes.MI_CUENTA_SCREEN)
    }

    fun onSignOutClick(openAndPopUp: (String, String) -> Unit) {
        launchCatching {
            // Eliminar el token de este dispositivo ANTES de cerrar sesión,
            // para no seguir enviando push a un usuario que ya salió.
            notificacionService.eliminarTokenActual()
            accountService.signOut()
            openAndPopUp(AparkauRoutes.LOGIN_SCREEN, AparkauRoutes.HOME_SCREEN)
        }
    }

    private fun cargarUsuario() {
        uiState.value = uiState.value.copy(isLoading = true)
        launchCatching {
            try {
                val usuario = usuarioService.getUsuario(accountService.currentUserId)
                if (usuario != null) {
                    uiState.value = uiState.value.copy(
                        email = usuario.email,
                        perfil = usuario.perfil
                    )
                }
            } finally {
                uiState.value = uiState.value.copy(isLoading = false)
            }
        }
    }
}

