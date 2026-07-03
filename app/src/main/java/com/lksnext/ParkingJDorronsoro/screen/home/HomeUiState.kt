package com.lksnext.ParkingJDorronsoro.screen.home

import com.lksnext.ParkingJDorronsoro.model.Reserva

/**
 * Reserva del usuario junto con la información necesaria para el aviso "SOS":
 * si hoy está bloqueado (alguien ocupa la plaza que le bloquea) y si ya envió
 * el aviso (solo se permite uno por sesión).
 */
data class ReservaHomeUi(
    val reserva: Reserva,
    val zona: String = "",
    val esHoy: Boolean = false,
    val esTandem: Boolean = false,
    val bloqueado: Boolean = false,
    val avisoEnviado: Boolean = false,
    // El compañero al que ESTA reserva bloquea ha pedido salir: mostramos un
    // aviso en la tarjeta para que el bloqueador mueva el coche.
    val avisoSalidaPendiente: Boolean = false
)

data class HomeUiState(
    val userId: String = "",
    val reservas: List<ReservaHomeUi> = emptyList(),
    val isLoading: Boolean = false
)
