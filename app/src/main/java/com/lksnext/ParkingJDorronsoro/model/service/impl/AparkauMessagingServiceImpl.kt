package com.lksnext.ParkingJDorronsoro.model.service.impl

import android.util.Log
import com.lksnext.ParkingJDorronsoro.R
import com.lksnext.ParkingJDorronsoro.model.service.AccountService
import com.lksnext.ParkingJDorronsoro.model.service.AparkauMessagingService
import com.lksnext.ParkingJDorronsoro.model.service.NotificacionService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Punto de entrada de Firebase Cloud Messaging en Android. El sistema instancia
 * esta clase, por eso debe ser concreta y heredar de [FirebaseMessagingService].
 * Implementa además la interfaz [AparkauMessagingService], siguiendo el patrón
 * interfaz + impl del proyecto.
 *
 * Toda la lógica reutilizable (guardar token, mostrar notificación) vive en
 * [NotificacionService].
 */
@AndroidEntryPoint
class AparkauMessagingServiceImpl : FirebaseMessagingService(), AparkauMessagingService {

    @Inject
    lateinit var notificacionService: NotificacionService

    @Inject
    lateinit var accountService: AccountService

    // Scope propio del servicio para las operaciones asíncronas con Firestore.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "AparkauFCM"
    }

    /**
     * FCM llama a este método al generar un token nuevo o al renovarlo.
     * Delegamos su persistencia en el servicio (que solo lo guarda si hay
     * usuario con sesión iniciada).
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken: $token")
        serviceScope.launch {
            notificacionService.registrarToken(token)
        }
    }

    /**
     * Se invoca cuando llega un mensaje estando la app en primer plano, o siempre
     * que el mensaje contenga un payload de tipo "data".
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "onMessageReceived de=${message.from} notif=${message.notification} data=${message.data}")

        // Defensa anti "token heredado": si el mensaje indica destinatario y el
        // usuario con sesión iniciada NO es ese destinatario, descartamos. Evita
        // que un aviso destinado a otra cuenta se muestre en este dispositivo
        // (p. ej. tras cambiar de usuario en el mismo móvil).
        val uidDestino = message.data["uidDestino"]
        if (uidDestino != null) {
            val uidActual = if (accountService.hasUser) accountService.currentUserId else null
            if (uidDestino != uidActual) {
                Log.d(TAG, "Aviso para $uidDestino pero el usuario actual es $uidActual; se ignora.")
                return
            }
        }

        // Preferimos el bloque "notification"; si no, caemos al payload "data".
        val titulo = message.notification?.title
            ?: message.data["titulo"]
            ?: getString(R.string.app_name)

        val cuerpo = message.notification?.body
            ?: message.data["cuerpo"]
            ?: run {
                Log.w(TAG, "Mensaje sin cuerpo; no se muestra notificación")
                return
            }

        notificacionService.mostrarNotificacion(titulo, cuerpo)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

