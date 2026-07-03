package com.lksnext.ParkingJDorronsoro.screen.mis_coches

import com.lksnext.ParkingJDorronsoro.model.Vehiculo

data class MisCochesUiState(
    val matricula: String = "",
    val modelo: String = "",
    val vehiculos: List<Vehiculo> = emptyList(),
    val isLoading: Boolean = false
)

