package com.lksnext.ParkingJDorronsoro.model.service.impl

import com.lksnext.ParkingJDorronsoro.model.EstadoReserva
import com.lksnext.ParkingJDorronsoro.model.Reserva
import com.lksnext.ParkingJDorronsoro.model.service.ReservaService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReservaServiceImpl @Inject constructor() : ReservaService {

    // Lazy para que la instancia se cree una sola vez cuando se necesite
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    companion object {
        private const val COLECCION_RESERVAS = "reservas"
        private const val CAMPO_USUARIO_ID = "usuarioId"
    }

    /**
     * Crea una nueva reserva en Firestore y devuelve el ID del documento generado.
     */
    override suspend fun crearReserva(reserva: Reserva): String {
        return firestore
            .collection(COLECCION_RESERVAS)
            .add(reserva)
            .await()
            .id
    }

    /**
     * Devuelve las reservas de un usuario que todavía no han finalizado
     * (estado AGENDADA o ACTIVA).
     */
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

    /**
     * Devuelve TODAS las reservas activas (de cualquier usuario),
     * útil para mostrar quién ocupa cada plaza.
     */
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

    /**
     * Elimina una reserva de Firestore a partir de su ID.
     */
    override suspend fun eliminarReserva(reservaId: String) {
        firestore
            .collection(COLECCION_RESERVAS)
            .document(reservaId)
            .delete()
            .await()
    }

    /**
     * Limpia el campo `avisoSalidaEn` de la reserva: el bloqueador ya ha visto
     * el aviso de "alguien necesita salir".
     */
    override suspend fun marcarAvisoSalidaVisto(reservaId: String) {
        firestore
            .collection(COLECCION_RESERVAS)
            .document(reservaId)
            .update("avisoSalidaEn", null)
            .await()
    }
}
