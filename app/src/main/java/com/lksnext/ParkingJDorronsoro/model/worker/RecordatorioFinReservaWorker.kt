package com.lksnext.ParkingJDorronsoro.model.worker

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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lksnext.ParkingJDorronsoro.MainActivity
import com.lksnext.ParkingJDorronsoro.R

/**
 * Worker genérico de recordatorio de reserva.
 * Se reutiliza para DOS avisos:
 *  - 30 minutos antes de que COMIENCE la reserva  (TAG_PREFIJO_INICIO)
 *  - 15 minutos antes de que FINALICE la reserva  (TAG_PREFIJO_FIN)
 *
 * No necesita FCM ni Cloud Functions: WorkManager persiste la tarea aunque
 * el proceso muera y la dispara a la hora exacta programada.
 */
class RecordatorioFinReservaWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val titulo = inputData.getString(KEY_TITULO) ?: return Result.failure()
        val cuerpo  = inputData.getString(KEY_CUERPO)  ?: return Result.failure()

        crearCanalSiNoExiste()

        // Android 13+ requiere permiso POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val concedido = ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!concedido) return Result.success() // sin permiso, no mostramos nada
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificacion = NotificationCompat.Builder(
            applicationContext,
            applicationContext.getString(R.string.notif_channel_tandem_id)
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(Math.abs(id.hashCode()), notificacion)

        Log.d("AparkauFCM", "Worker: notificación mostrada — $titulo")
        return Result.success()
    }

    private fun crearCanalSiNoExiste() {
        val manager = applicationContext
            .getSystemService(NotificationManager::class.java) ?: return
        val canalId = applicationContext.getString(R.string.notif_channel_tandem_id)
        if (manager.getNotificationChannel(canalId) != null) return

        val canal = NotificationChannel(
            canalId,
            applicationContext.getString(R.string.notif_channel_tandem_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = applicationContext.getString(R.string.notif_channel_tandem_desc)
        }
        manager.createNotificationChannel(canal)
    }

    companion object {
        const val KEY_TITULO  = "titulo"
        const val KEY_CUERPO  = "cuerpo"

        /** Prefijo para el recordatorio de INICIO (30 min antes de horaInicio). */
        const val TAG_PREFIJO_INICIO = "recordatorio_inicio_"
        /** Minutos de antelación antes del INICIO de la reserva. */
        const val MINUTOS_ANTES_INICIO = 30L

        /** Prefijo para el recordatorio de FIN (15 min antes de horaFin). */
        const val TAG_PREFIJO_FIN = "recordatorio_fin_"
        /** Minutos de antelación antes del FIN de la reserva. */
        const val MINUTOS_ANTES_FIN = 15L

        // Alias de compatibilidad (usado en el código existente)
        @Deprecated("Usa TAG_PREFIJO_FIN", ReplaceWith("TAG_PREFIJO_FIN"))
        const val TAG_PREFIJO   = TAG_PREFIJO_FIN
        @Deprecated("Usa MINUTOS_ANTES_FIN", ReplaceWith("MINUTOS_ANTES_FIN"))
        const val MINUTOS_ANTES = MINUTOS_ANTES_FIN
    }
}

