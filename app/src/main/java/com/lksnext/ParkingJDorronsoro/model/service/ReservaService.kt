package com.lksnext.ParkingJDorronsoro.model.service

import com.google.firebase.Timestamp
import com.lksnext.ParkingJDorronsoro.model.Reserva

interface ReservaService {
    suspend fun crearReserva(reserva: Reserva): String
    suspend fun getReserva(reservaId: String): Reserva?
    suspend fun getReservasActivas(usuarioId: String): List<Reserva>
    suspend fun getTodasLasReservasActivas(): List<Reserva>
    suspend fun eliminarReserva(reservaId: String)
    suspend fun actualizarReserva(
        reservaId: String,
        plazaId: String,
        matriculaVehiculo: String,
        fechaReserva: Timestamp,
        horaInicio: Timestamp,
        horaFin: Timestamp
    )
    /** Limpia el aviso de salida de una reserva (el bloqueador lo ha visto). */
    suspend fun marcarAvisoSalidaVisto(reservaId: String)
    // Aquí irán las funciones del algoritmo Tetris
}