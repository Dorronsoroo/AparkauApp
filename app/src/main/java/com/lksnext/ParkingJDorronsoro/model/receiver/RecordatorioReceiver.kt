package com.lksnext.ParkingJDorronsoro.model.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
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

/**
 * Receptor que dispara AlarmManager a la hora EXACTA programada para mostrar
 * el recordatorio de una reserva (30 min antes del inicio / 15 min antes del fin).
 *
 * A diferencia de WorkManager, AlarmManager con
 * setExactAndAllowWhileIdle despierta el dispositivo incluso en modo Doze, por
 * lo que la notificación llega en el momento justo.
 */
class RecordatorioReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val titulo = intent.getStringExtra(EXTRA_TITULO) ?: return
        val cuerpo = intent.getStringExtra(EXTRA_CUERPO) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 1)

        crearCanalSiNoExiste(context)

        // Android 13+ requiere permiso POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val concedido = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!concedido) {
                Log.w("AparkauFCM", "Recordatorio: permiso POST_NOTIFICATIONS denegado")
                return
            }
        }

        val abrirApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notifId, abrirApp,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificacion = NotificationCompat.Builder(
            context, context.getString(R.string.notif_channel_tandem_id)
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notificacion)
        Log.d("AparkauFCM", "Recordatorio (AlarmManager) mostrado — $titulo")
    }

    private fun crearCanalSiNoExiste(context: Context) {
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

    companion object {
        const val EXTRA_TITULO = "extra_titulo"
        const val EXTRA_CUERPO = "extra_cuerpo"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
        const val ACTION_RECORDATORIO = "com.lksnext.ParkingJDorronsoro.RECORDATORIO"
    }
}


