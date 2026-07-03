package com.lksnext.ParkingJDorronsoro

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Refresca el token FCM en cada arranque si ya hay sesión iniciada.
        // Esto evita que queden tokens obsoletos en Firestore cuando el usuario
        // no vuelve a hacer login (el token FCM puede rotar o cambiar tras
        // reinstalar/actualizar/limpiar datos), lo que provocaría envíos que FCM
        // acepta pero nunca entrega al dispositivo.
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


