package com.lksnext.ParkingJDorronsoro.model.service.impl

import com.lksnext.ParkingJDorronsoro.model.Plaza
import com.lksnext.ParkingJDorronsoro.model.service.PlazaService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PlazaServiceImpl @Inject constructor() : PlazaService {

    // Lazy para que la instancia se cree una sola vez cuando se necesite
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    companion object {
        private const val COLECCION_PLAZAS = "plazas"
        private const val CAMPO_ESTADO = "estado"
    }

    /**
     * Devuelve todas las plazas del parking ordenadas por su ID (número de plaza).
     */
    override suspend fun getTodasLasPlazas(): List<Plaza> {
        return firestore
            .collection(COLECCION_PLAZAS)
            .get(Source.SERVER)
            .await()
            .toObjects(Plaza::class.java)
    }

    /**
     * Obtiene una plaza concreta por su ID (número de plaza), o null si no existe.
     */
    override suspend fun getPlaza(plazaId: String): Plaza? {
        return firestore
            .collection(COLECCION_PLAZAS)
            .document(plazaId)
            .get(Source.SERVER)
            .await()
            .toObject(Plaza::class.java)
    }

    /**
     * Actualiza únicamente el campo "estado" de una plaza (LIBRE, OCUPADA, BLOQUEADA_POR_TANDEM).
     */
    override suspend fun actualizarEstadoPlaza(plazaId: String, nuevoEstado: String) {
        firestore
            .collection(COLECCION_PLAZAS)
            .document(plazaId)
            .update(CAMPO_ESTADO, nuevoEstado)
            .await()
    }
}
