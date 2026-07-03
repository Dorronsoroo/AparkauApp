package com.lksnext.ParkingJDorronsoro.screen.mis_coches


import androidx.compose.runtime.mutableStateOf
import com.lksnext.ParkingJDorronsoro.AparkauRoutes
import com.lksnext.ParkingJDorronsoro.MakeItSoViewModel
import com.lksnext.ParkingJDorronsoro.common.AppText
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarManager
import com.lksnext.ParkingJDorronsoro.model.Vehiculo
import com.lksnext.ParkingJDorronsoro.model.service.AccountService
import com.lksnext.ParkingJDorronsoro.model.service.LogService
import com.lksnext.ParkingJDorronsoro.model.service.VehiculoService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MisCochesViewModel @Inject constructor(
    private val accountService: AccountService,
    private val vehiculoService: VehiculoService,
    logService: LogService
) : MakeItSoViewModel(logService) {

    var uiState = mutableStateOf(MisCochesUiState())
        private set

    private val matricula get() = uiState.value.matricula
    private val modelo get() = uiState.value.modelo

    init {
        cargarVehiculos()
    }

    fun onMatriculaChange(newValue: String) {
        uiState.value = uiState.value.copy(matricula = newValue.uppercase())
    }

    fun onModeloChange(newValue: String) {
        uiState.value = uiState.value.copy(modelo = newValue)
    }

    fun onAddVehiculoClick() {
        if (matricula.isBlank()) {
            SnackbarManager.showMessage(AppText.matricula_error)
            return
        }
        if (modelo.isBlank()) {
            SnackbarManager.showMessage(AppText.modelo_error)
            return
        }

        launchCatching {
            val vehiculo = Vehiculo(
                matricula = matricula.trim(),
                modelo = modelo.trim()
            )

            val resultado = vehiculoService.agregarVehiculo(
                uid = accountService.currentUserId,
                vehiculo = vehiculo
            )
            resultado.getOrElse { error -> throw error }

            SnackbarManager.showMessage(AppText.vehiculo_guardado)
            uiState.value = uiState.value.copy(matricula = "", modelo = "")
            cargarVehiculos()
        }
    }

    fun onEliminarVehiculoClick(vehiculo: Vehiculo) {
        launchCatching {
            val resultado = vehiculoService.eliminarVehiculo(
                uid = accountService.currentUserId,
                vehiculo = vehiculo
            )
            resultado.getOrElse { error -> throw error }
            cargarVehiculos()
        }
    }

    fun onVolverClick(openAndPopUp: (String, String) -> Unit) {
        openAndPopUp(AparkauRoutes.MI_CUENTA_SCREEN, AparkauRoutes.MIS_COCHES_SCREEN)
    }

    private fun cargarVehiculos() {
        uiState.value = uiState.value.copy(isLoading = true)
        launchCatching {
            try {
                val vehiculos = vehiculoService.getVehiculos(accountService.currentUserId)
                uiState.value = uiState.value.copy(vehiculos = vehiculos)
            } finally {
                uiState.value = uiState.value.copy(isLoading = false)
            }
        }
    }
}

