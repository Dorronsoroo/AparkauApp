package com.lksnext.ParkingJDorronsoro.screen.sign_up

import com.lksnext.ParkingJDorronsoro.model.PerfilUsuario

data class SignUpUiState(
    val nombre: String = "",
    val apellidos: String = "",
    val email: String = "",
    val password: String = "",
    val repeatPassword: String = "",
    val perfil: PerfilUsuario = PerfilUsuario.EMPLEADO_HABITUAL
)
