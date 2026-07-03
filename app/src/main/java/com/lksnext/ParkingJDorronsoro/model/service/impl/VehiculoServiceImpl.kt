package com.lksnext.ParkingJDorronsoro.model.service.impl

import com.lksnext.ParkingJDorronsoro.model.Usuario
import com.lksnext.ParkingJDorronsoro.model.Vehiculo
import com.lksnext.ParkingJDorronsoro.model.service.VehiculoService
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class VehiculoServiceImpl @Inject constructor() : VehiculoService {

    // Lazy para que la instancia se cree una sola vez cuando se necesite
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    companion object {
        private const val COLECCION_USUARIOS = "usuarios"
        private const val CAMPO_VEHICULOS = "vehiculos"
    }

    /**
     * Lee el documento del usuario y devuelve su lista de vehículos.
     */
    override suspend fun getVehiculos(uid: String): List<Vehiculo> {
        val snapshot = firestore
            .collection(COLECCION_USUARIOS)
            .document(uid)
            .get()
            .await()

        return snapshot.toObject(Usuario::class.java)?.vehiculos ?: emptyList()
    }

    /**
     * Añade el vehículo al array "vehiculos" del documento del usuario.
     *
     * Se usa set(..., merge) en lugar de update() porque update() falla si el
     * documento del usuario todavía no existe en Firestore. Con merge, si el
     * documento (y la colección) no existen, se crean automáticamente; y si ya
     * existen, solo se actualiza el campo "vehiculos" sin borrar los demás.
     */
    override suspend fun agregarVehiculo(uid: String, vehiculo: Vehiculo): Result<Unit> {
        return try {
            firestore
                .collection(COLECCION_USUARIOS)
                .document(uid)
                .set(
                    mapOf(CAMPO_VEHICULOS to FieldValue.arrayUnion(vehiculo)),
                    SetOptions.merge()
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina el vehículo del array "vehiculos" del documento del usuario.
     */
    override suspend fun eliminarVehiculo(uid: String, vehiculo: Vehiculo): Result<Unit> {
        return try {
            firestore
                .collection(COLECCION_USUARIOS)
                .document(uid)
                .set(
                    mapOf(CAMPO_VEHICULOS to FieldValue.arrayRemove(vehiculo)),
                    SetOptions.merge()
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

