package com.lksnext.ParkingJDorronsoro.model.service.impl

import com.lksnext.ParkingJDorronsoro.model.PerfilUsuario
import com.lksnext.ParkingJDorronsoro.model.Usuario
import com.lksnext.ParkingJDorronsoro.model.service.UsuarioService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UsuarioServiceImpl @Inject constructor() : UsuarioService {

    // Lazy para que la instancia se cree una sola vez cuando se necesite
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    companion object {
        private const val COLECCION_USUARIOS = "usuarios"
        private const val SUBCOLECCION_TOKENS = "tokens"
        private const val CAMPO_EMAIL = "email"
        private const val CAMPO_PERFIL = "perfil"
    }

    /**
     * Guarda el objeto [Usuario] en Firestore.
     * El ID del documento es exactamente el UID de FirebaseAuth,
     * garantizando así la coherencia entre ambos sistemas.
     */
    override suspend fun guardarUsuario(
        uid: String,
        nombre: String,
        apellidos: String,
        email: String,
        perfil: PerfilUsuario
    ): Result<Unit> {
        return try {
            val nuevoUsuario = Usuario(
                id = uid,
                nombre = nombre,
                apellidos = apellidos,
                email = email,
                perfil = perfil,
                vehiculos = emptyList()
            )

            firestore
                .collection(COLECCION_USUARIOS)
                .document(uid)          // El ID del documento == UID de Auth
                .set(nuevoUsuario)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUsuario(uid: String): Usuario? {
        return try {
            firestore
                .collection(COLECCION_USUARIOS)
                .document(uid)
                .get(Source.SERVER)
                .await()
                .toObject(Usuario::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Actualiza solo email y perfil usando merge, para no borrar la lista de
     * vehículos u otros campos del documento del usuario.
     */
    override suspend fun actualizarUsuario(
        uid: String,
        email: String,
        perfil: PerfilUsuario
    ): Result<Unit> {
        return try {
            firestore
                .collection(COLECCION_USUARIOS)
                .document(uid)
                .set(
                    mapOf(
                        CAMPO_EMAIL to email,
                        CAMPO_PERFIL to perfil.name
                    ),
                    SetOptions.merge()
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun guardarTokenFcm(uid: String, token: String): Result<Unit> {
        if (uid.isBlank() || token.isBlank()) {
            return Result.failure(IllegalArgumentException("uid o token vacío"))
        }
        return try {
            firestore
                .collection(COLECCION_USUARIOS)
                .document(uid)
                .collection(SUBCOLECCION_TOKENS)
                .document(token)          // El ID del documento es el propio token (idempotente)
                .set(
                    mapOf(
                        "token" to token,
                        "plataforma" to "android",
                        "actualizadoEn" to com.google.firebase.Timestamp.now()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun eliminarTokenFcm(uid: String, token: String): Result<Unit> {
        if (uid.isBlank() || token.isBlank()) {
            return Result.failure(IllegalArgumentException("uid o token vacío"))
        }
        return try {
            firestore
                .collection(COLECCION_USUARIOS)
                .document(uid)
                .collection(SUBCOLECCION_TOKENS)
                .document(token)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
