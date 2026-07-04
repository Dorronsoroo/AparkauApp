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
import android.app.AlarmManager
import com.lksnext.ParkingJDorronsoro.model.receiver.RecordatorioReceiver
import com.lksnext.ParkingJDorronsoro.model.worker.RecordatorioFinReservaWorker
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
            .notify(Random.nextInt(1, Int.MAX_VALUE), notificacion)
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

    override fun programarRecordatorios(
        reservaId: String,
        horaInicioMs: Long,
        horaFinMs: Long,
        plazaId: String
    ) {
        crearCanalSiNoExiste()

        // --- Recordatorio 1: 30 min antes del INICIO ---
        val disparoInicio = horaInicioMs -
                (RecordatorioFinReservaWorker.MINUTOS_ANTES_INICIO * 60 * 1000L)
        if (disparoInicio > System.currentTimeMillis()) {
            programarAlarma(
                requestCode = (RecordatorioFinReservaWorker.TAG_PREFIJO_INICIO + reservaId).hashCode(),
                triggerAtMs = disparoInicio,
                titulo = "Tu reserva empieza pronto",
                cuerpo = "Tu reserva de la plaza $plazaId comienza en ${RecordatorioFinReservaWorker.MINUTOS_ANTES_INICIO} minutos."
            )
            Log.d("AparkauFCM", "Recordatorio INICIO programado para reserva $reservaId plaza $plazaId")
        } else {
            Log.d("AparkauFCM", "Recordatorio INICIO omitido (ya pasó) para reserva $reservaId")
        }

        // --- Recordatorio 2: 15 min antes del FIN ---
        val disparoFin = horaFinMs -
                (RecordatorioFinReservaWorker.MINUTOS_ANTES_FIN * 60 * 1000L)
        if (disparoFin > System.currentTimeMillis()) {
            programarAlarma(
                requestCode = (RecordatorioFinReservaWorker.TAG_PREFIJO_FIN + reservaId).hashCode(),
                triggerAtMs = disparoFin,
                titulo = "Tu reserva termina pronto",
                cuerpo = "Tu reserva de la plaza $plazaId finaliza en ${RecordatorioFinReservaWorker.MINUTOS_ANTES_FIN} minutos."
            )
            Log.d("AparkauFCM", "Recordatorio FIN programado para reserva $reservaId plaza $plazaId")
        } else {
            Log.d("AparkauFCM", "Recordatorio FIN omitido (ya pasó) para reserva $reservaId")
        }
    }

    override fun cancelarRecordatorios(reservaId: String) {
        cancelarAlarma((RecordatorioFinReservaWorker.TAG_PREFIJO_INICIO + reservaId).hashCode())
        cancelarAlarma((RecordatorioFinReservaWorker.TAG_PREFIJO_FIN + reservaId).hashCode())
    }

    /**
     * Programa una alarma EXACTA con [AlarmManager]. A diferencia de WorkManager,
     * setExactAndAllowWhileIdle despierta el dispositivo aunque esté en Doze, por
     * lo que la notificación llega en el minuto exacto.
     */
    private fun programarAlarma(requestCode: Int, triggerAtMs: Long, titulo: String, cuerpo: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = crearPendingIntentAlarma(requestCode, titulo, cuerpo)

        // En Android 12+ (API 31) las alarmas exactas requieren permiso especial.
        // Si no está concedido, caemos a una alarma inexacta (mejor que nada).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w("AparkauFCM", "Sin permiso de alarma exacta: se usa alarma aproximada")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
    }

    private fun cancelarAlarma(requestCode: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = crearPendingIntentAlarma(requestCode, null, null)
        alarmManager.cancel(pendingIntent)
    }

    private fun crearPendingIntentAlarma(requestCode: Int, titulo: String?, cuerpo: String?): PendingIntent {
        val intent = Intent(context, RecordatorioReceiver::class.java).apply {
            action = RecordatorioReceiver.ACTION_RECORDATORIO
            putExtra(RecordatorioReceiver.EXTRA_NOTIF_ID, requestCode)
            titulo?.let { putExtra(RecordatorioReceiver.EXTRA_TITULO, it) }
            cuerpo?.let { putExtra(RecordatorioReceiver.EXTRA_CUERPO, it) }
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
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






