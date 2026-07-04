package com.lksnext.ParkingJDorronsoro.model.service.impl

import com.google.firebase.Timestamp
import com.lksnext.ParkingJDorronsoro.model.EstadoReserva
import com.lksnext.ParkingJDorronsoro.model.Reserva
import com.lksnext.ParkingJDorronsoro.model.service.ReservaService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReservaServiceImpl @Inject constructor() : ReservaService {

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    companion object {
        private const val COLECCION_RESERVAS = "reservas"
        private const val CAMPO_USUARIO_ID = "usuarioId"
    }

    override suspend fun crearReserva(reserva: Reserva): String {
        return firestore
            .collection(COLECCION_RESERVAS)
            .add(reserva)
            .await()
            .id
    }

    override suspend fun getReserva(reservaId: String): Reserva? {
        return firestore
            .collection(COLECCION_RESERVAS)
            .document(reservaId)
            .get(Source.SERVER)
            .await()
            .toObject(Reserva::class.java)
    }

    override suspend fun getReservasActivas(usuarioId: String): List<Reserva> {
        return firestore
            .collection(COLECCION_RESERVAS)
            .whereEqualTo(CAMPO_USUARIO_ID, usuarioId)
            .get(Source.SERVER)
            .await()
            .toObjects(Reserva::class.java)
            .filter {
                it.estado == EstadoReserva.AGENDADA || it.estado == EstadoReserva.ACTIVA
            }
    }

    override suspend fun getTodasLasReservasActivas(): List<Reserva> {
        return firestore
            .collection(COLECCION_RESERVAS)
            .get(Source.SERVER)
            .await()
            .toObjects(Reserva::class.java)
            .filter {
                it.estado == EstadoReserva.AGENDADA || it.estado == EstadoReserva.ACTIVA
            }
    }

    override suspend fun eliminarReserva(reservaId: String) {
        firestore
            .collection(COLECCION_RESERVAS)
            .document(reservaId)
            .delete()
            .await()
    }

    override suspend fun actualizarReserva(
        reservaId: String,
        plazaId: String,
        matriculaVehiculo: String,
        fechaReserva: Timestamp,
        horaInicio: Timestamp,
        horaFin: Timestamp
    ) {
        firestore
            .collection(COLECCION_RESERVAS)
            .document(reservaId)
            .update(
                mapOf(
                    "plazaId" to plazaId,
                    "matriculaVehiculo" to matriculaVehiculo,
                    "fechaReserva" to fechaReserva,
                    "horaInicio" to horaInicio,
                    "horaFin" to horaFin
                )
            )
            .await()
    }

    override suspend fun marcarAvisoSalidaVisto(reservaId: String) {
        firestore
            .collection(COLECCION_RESERVAS)
            .document(reservaId)
            .update("avisoSalidaEn", null)
            .await()
    }
}
