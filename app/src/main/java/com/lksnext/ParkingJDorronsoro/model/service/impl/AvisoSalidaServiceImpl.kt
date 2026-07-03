package com.lksnext.ParkingJDorronsoro.model.service.impl

import com.lksnext.ParkingJDorronsoro.model.service.AvisoSalidaService
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AvisoSalidaServiceImpl @Inject constructor() : AvisoSalidaService {

    // Lazy para que la instancia se cree una sola vez cuando se necesite
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    companion object {
        private const val COLECCION_AVISOS = "avisosSalida"
        private const val CAMPO_USUARIO_ID = "usuarioId"
        private const val CAMPO_PLAZA_BLOQUEADA_ID = "plazaBloqueadaId"
        private const val CAMPO_CREADO_EN = "creadoEn"
    }

    override suspend fun solicitarSalida(usuarioId: String, plazaBloqueadaId: String) {
        val aviso = hashMapOf(
            CAMPO_USUARIO_ID to usuarioId,
            CAMPO_PLAZA_BLOQUEADA_ID to plazaBloqueadaId,
            CAMPO_CREADO_EN to FieldValue.serverTimestamp()
        )
        firestore
            .collection(COLECCION_AVISOS)
            .add(aviso)
            .await()
    }
}

