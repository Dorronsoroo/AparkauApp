package com.lksnext.ParkingJDorronsoro.screen.home

import androidx.compose.runtime.mutableStateOf
import com.lksnext.ParkingJDorronsoro.AparkauRoutes
import com.lksnext.ParkingJDorronsoro.MakeItSoViewModel
import com.lksnext.ParkingJDorronsoro.common.AppText
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarManager
import com.lksnext.ParkingJDorronsoro.model.service.AccountService
import com.lksnext.ParkingJDorronsoro.model.service.AvisoSalidaService
import com.lksnext.ParkingJDorronsoro.model.service.LogService
import com.lksnext.ParkingJDorronsoro.model.service.PlazaService
import com.lksnext.ParkingJDorronsoro.model.service.ReservaService
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accountService: AccountService,
    private val reservaService: ReservaService,
    private val plazaService: PlazaService,
    private val avisoSalidaService: AvisoSalidaService,
    logService: LogService
) : MakeItSoViewModel(logService) {

    var uiState = mutableStateOf(HomeUiState())
        private set

    init {
        uiState.value = uiState.value.copy(userId = accountService.currentUserId)
        cargarReservas()
    }

    private fun cargarReservas() {
        uiState.value = uiState.value.copy(isLoading = true)
        launchCatching {
            try {
                val uid = accountService.currentUserId
                val misReservas = reservaService.getReservasActivas(uid)
                    .sortedBy { it.horaInicio }
                val plazas = plazaService.getTodasLasPlazas()
                val todasReservas = reservaService.getTodasLasReservasActivas()

                // Plazas que HOY están ocupadas por OTRO usuario (solo presencia,
                // nunca identidad): sirve para saber si el "coche de delante" llegó.
                val plazasOcupadasPorOtros = todasReservas
                    .filter { it.usuarioId != uid && esHoy(it.fechaReserva ?: it.horaInicio) }
                    .map { it.plazaId }
                    .toSet()

                // Para cada plaza trasera B, la plaza A que la bloquea (A.plazaBloqueadaId == B).
                val bloqueadoraPorPlaza = plazas
                    .filter { it.plazaBloqueadaId.isNotBlank() }
                    .associate { it.plazaBloqueadaId to it.id }

                // Avisos ya enviados en esta sesión (solo se permite uno por plaza).
                val avisosPrevios = uiState.value.reservas
                    .filter { it.avisoEnviado }
                    .map { it.reserva.plazaId }
                    .toSet()

                val reservasUi = misReservas.map { reserva ->
                    val esHoy = esHoy(reserva.fechaReserva ?: reserva.horaInicio)
                    val plazaBloqueadoraId = bloqueadoraPorPlaza[reserva.plazaId]
                    val bloqueado = esHoy &&
                        plazaBloqueadoraId != null &&
                        plazaBloqueadoraId in plazasOcupadasPorOtros
                    ReservaHomeUi(
                        reserva = reserva,
                        zona = plazas.firstOrNull { it.id == reserva.plazaId }?.zona.orEmpty(),
                        esHoy = esHoy,
                        esTandem = plazaBloqueadoraId != null,
                        bloqueado = bloqueado,
                        avisoEnviado = reserva.plazaId in avisosPrevios,
                        // La Cloud Function marca `avisoSalidaEn` en la reserva del
                        // bloqueador cuando el compañero de detrás pide salir.
                        avisoSalidaPendiente = reserva.avisoSalidaEn != null &&
                            esHoy(reserva.avisoSalidaEn)
                    )
                }

                uiState.value = uiState.value.copy(reservas = reservasUi)
            } finally {
                uiState.value = uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onReserveClick(openScreen: (String) -> Unit) {
        openScreen(AparkauRoutes.RESERVA_SCREEN)
    }

    fun onEliminarReservaClick(reservaId: String) {
        launchCatching {
            reservaService.eliminarReserva(reservaId)
            uiState.value = uiState.value.copy(
                reservas = uiState.value.reservas.filterNot { it.reserva.id == reservaId }
            )
            SnackbarManager.showMessage(AppText.reserva_eliminada)
        }
    }

    /**
     * Envía un aviso ANÓNIMO al usuario que bloquea la plaza [plazaBloqueadaId].
     * Solo se permite un aviso: tras enviarlo el botón queda deshabilitado.
     */
    fun onAvisarSalidaClick(plazaBloqueadaId: String) {
        // Guardas defensivas: ya enviado, o no está realmente bloqueado.
        val objetivo = uiState.value.reservas.firstOrNull { it.reserva.plazaId == plazaBloqueadaId }
        if (objetivo == null || objetivo.avisoEnviado || !objetivo.bloqueado) return

        // Marcamos como enviado de inmediato para impedir un segundo aviso.
        uiState.value = uiState.value.copy(
            reservas = uiState.value.reservas.map {
                if (it.reserva.plazaId == plazaBloqueadaId) it.copy(avisoEnviado = true) else it
            }
        )

        launchCatching {
            avisoSalidaService.solicitarSalida(accountService.currentUserId, plazaBloqueadaId)
            SnackbarManager.showMessage(AppText.aviso_salida_enviado)
        }
    }

    fun onMiCuentaClick(openScreen: (String) -> Unit) {
        openScreen(AparkauRoutes.MI_CUENTA_SCREEN)
    }

    /**
     * El bloqueador confirma que ha visto el aviso "alguien necesita salir".
     * Limpia el aviso en Firestore y oculta el banner de esa reserva.
     */
    fun onAvisoSalidaVistoClick(reservaId: String) {
        uiState.value = uiState.value.copy(
            reservas = uiState.value.reservas.map {
                if (it.reserva.id == reservaId) it.copy(avisoSalidaPendiente = false) else it
            }
        )
        launchCatching {
            reservaService.marcarAvisoSalidaVisto(reservaId)
        }
    }

    private fun esHoy(timestamp: Timestamp?): Boolean {
        val fecha = timestamp?.toDate() ?: return false
        return esMismoDia(fecha, Date())
    }

    private fun esMismoDia(a: Date, b: Date): Boolean {
        val calA = Calendar.getInstance().apply { time = a }
        val calB = Calendar.getInstance().apply { time = b }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }
}
