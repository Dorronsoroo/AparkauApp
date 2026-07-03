package com.lksnext.ParkingJDorronsoro.screen.mi_cuenta

import com.lksnext.ParkingJDorronsoro.model.PerfilUsuario

data class MiCuentaUiState(
    val email: String = "",
    val perfil: PerfilUsuario = PerfilUsuario.EMPLEADO_HABITUAL,
    val isLoading: Boolean = false
)

