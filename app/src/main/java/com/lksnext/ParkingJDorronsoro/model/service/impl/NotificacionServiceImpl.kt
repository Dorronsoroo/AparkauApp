package com.lksnext.ParkingJDorronsoro.model.service.impl

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lksnext.ParkingJDorronsoro.MainActivity
import com.lksnext.ParkingJDorronsoro.R
import com.lksnext.ParkingJDorronsoro.model.service.AccountService
import com.lksnext.ParkingJDorronsoro.model.service.NotificacionService
import com.lksnext.ParkingJDorronsoro.model.service.UsuarioService
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.random.Random

class NotificacionServiceImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val accountService: AccountService,
    private val usuarioService: UsuarioService
) : NotificacionService {

    override fun mostrarNotificacion(titulo: String, cuerpo: String) {
        crearCanalSiNoExiste()

        // En Android 13+ (API 33) se necesita el permiso POST_NOTIFICATIONS.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val concedido = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!concedido) {
                Log.w("AparkauFCM", "Permiso POST_NOTIFICATIONS DENEGADO: no se muestra la notificación")
                return
            }
        }

        val notificacion = NotificationCompat.Builder(
            context,
            context.getString(R.string.notif_channel_tandem_id)
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(crearIntentApertura())
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(Random.nextInt(), notificacion)
    }

    override suspend fun registrarToken(token: String) {
        if (!accountService.hasUser) return
        usuarioService.guardarTokenFcm(accountService.currentUserId, token)
    }

    override suspend fun registrarTokenActual() {
        if (!accountService.hasUser) return
        val token = FirebaseMessaging.getInstance().token.await()
        usuarioService.guardarTokenFcm(accountService.currentUserId, token)
    }

    override suspend fun eliminarTokenActual() {
        if (!accountService.hasUser) return
        val token = FirebaseMessaging.getInstance().token.await()
        usuarioService.eliminarTokenFcm(accountService.currentUserId, token)
    }

    /**
     * Crea un PendingIntent que abre la app (MainActivity) al tocar la
     * notificación. Sin esto, pulsar la notificación no hace nada.
     */
    private fun crearIntentApertura(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /** Crea el canal de notificaciones (obligatorio en Android 8+). */
    private fun crearCanalSiNoExiste() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val canalId = context.getString(R.string.notif_channel_tandem_id)
        if (manager.getNotificationChannel(canalId) != null) return

        val canal = NotificationChannel(
            canalId,
            context.getString(R.string.notif_channel_tandem_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_tandem_desc)
        }
        manager.createNotificationChannel(canal)
    }
}






