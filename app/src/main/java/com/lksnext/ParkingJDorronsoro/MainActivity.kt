package com.lksnext.ParkingJDorronsoro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.lksnext.ParkingJDorronsoro.model.service.NotificacionService
import com.lksnext.ParkingJDorronsoro.theme.AparkauTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var notificacionService: NotificacionService

    // Lanzador del diálogo de permiso POST_NOTIFICATIONS (Android 13+)
    private val solicitarPermisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            if (concedido) {
                Log.d("AparkauFCM", "Permiso POST_NOTIFICATIONS concedido.")
            } else {
                Log.w("AparkauFCM", "Permiso POST_NOTIFICATIONS denegado: las notificaciones no se mostrarán.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Android 13+ requiere pedir POST_NOTIFICATIONS en tiempo de ejecución.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val yaPermitido = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!yaPermitido) {
                solicitarPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Refresca el token FCM en cada arranque si ya hay sesión iniciada.
        lifecycleScope.launch {
            runCatching { notificacionService.registrarTokenActual() }
                .onFailure { Log.w("AparkauFCM", "No se pudo registrar el token en el arranque", it) }
        }

        setContent {
            AparkauTheme {
                AparkauNavGraph()
            }
        }
    }
}


