package com.lksnext.ParkingJDorronsoro.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Reserva(
    @DocumentId val id: String = "",
    val usuarioId: String = "",
    val plazaId: String = "",
    val matriculaVehiculo: String = "",
    val estado: EstadoReserva = EstadoReserva.AGENDADA,
    val fechaReserva: Timestamp? = null,
    val horaInicio: Timestamp? = null,
    val horaFin: Timestamp? = null,
    val horaSalidaEstimada: Timestamp? = null,
    val checkInReal: Timestamp? = null,
    val checkOutReal: Timestamp? = null,
    // Marca (fijada por la Cloud Function) de cuándo un compañero al que esta
    // reserva bloquea ha pedido salir. La app muestra un aviso en la tarjeta
    // mientras esté presente; se limpia (null) cuando el bloqueador lo confirma.
    val avisoSalidaEn: Timestamp? = null
)
