package com.lksnext.ParkingJDorronsoro.screen.reserva

import androidx.compose.runtime.mutableStateOf
import com.lksnext.ParkingJDorronsoro.AparkauRoutes
import com.lksnext.ParkingJDorronsoro.common.AppText
import com.lksnext.ParkingJDorronsoro.MakeItSoViewModel
import com.lksnext.ParkingJDorronsoro.model.EstadoPlaza
import com.lksnext.ParkingJDorronsoro.model.EstadoReserva
import com.lksnext.ParkingJDorronsoro.model.Plaza
import com.lksnext.ParkingJDorronsoro.model.Reserva
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarManager
import com.lksnext.ParkingJDorronsoro.model.service.AccountService
import com.lksnext.ParkingJDorronsoro.model.service.LogService
import com.lksnext.ParkingJDorronsoro.model.service.PlazaService
import com.lksnext.ParkingJDorronsoro.model.service.ReservaService
import com.lksnext.ParkingJDorronsoro.model.service.UsuarioService
import com.lksnext.ParkingJDorronsoro.model.service.VehiculoService
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ReservaViewModel @Inject constructor(
    private val accountService: AccountService,
    private val plazaService: PlazaService,
    private val reservaService: ReservaService,
    private val vehiculoService: VehiculoService,
    private val usuarioService: UsuarioService,
    logService: LogService
) : MakeItSoViewModel(logService) {

    var uiState = mutableStateOf(ReservaUiState())
        private set

    private val matricula get() = uiState.value.matriculaSeleccionada
    private val fecha get() = uiState.value.fecha
    private val horaInicio get() = uiState.value.horaInicio
    private val horaFin get() = uiState.value.horaFin

    // Datos base cargados de Firestore. La ocupación se deriva de estos según el día seleccionado.
    private var plazasBase: List<Plaza> = emptyList()
    private var reservasActivas: List<Reserva> = emptyList()
    private val nombreUsuarioCache = mutableMapOf<String, String>()

    companion object {
        private const val MAX_DIAS_ANTELACION = 7L
        private const val MAX_HORAS_DURACION = 9L
    }

    init {
        cargarPlazas()
        cargarVehiculos()
    }

    fun onVehiculoSelected(newValue: String) {
        uiState.value = uiState.value.copy(matriculaSeleccionada = newValue)
    }

    fun onFechaChange(newValue: LocalDate) {
        uiState.value = uiState.value.copy(fecha = newValue)
        // Al cambiar de día recalculamos qué plazas están ocupadas ESE día concreto.
        recalcularParaFecha(newValue)
    }

    fun onHoraInicioChange(newValue: LocalTime) {
        uiState.value = uiState.value.copy(horaInicio = newValue)
    }

    fun onHoraFinChange(newValue: LocalTime) {
        uiState.value = uiState.value.copy(horaFin = newValue)
    }

    fun onVolverClick(openAndPopUp: (String, String) -> Unit) {
        // Volvemos a Home recreándolo para que recargue la lista de reservas
        openAndPopUp(AparkauRoutes.HOME_SCREEN, AparkauRoutes.HOME_SCREEN)
    }

    private fun cargarPlazas() {
        uiState.value = uiState.value.copy(isLoading = true)
        launchCatching {
            try {
                plazasBase = plazaService.getTodasLasPlazas()
                    .sortedWith(
                        compareBy(
                            { it.id.takeWhile { c -> c.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE },
                            { it.id }
                        )
                    )

                // Cargamos todas las reservas activas (de todos los días) una sola vez.
                reservasActivas = reservaService.getTodasLasReservasActivas()

                // Precargamos los nombres de los ocupantes para no repetir consultas.
                reservasActivas
                    .map { it.usuarioId }
                    .filter { it.isNotBlank() && !nombreUsuarioCache.containsKey(it) }
                    .distinct()
                    .forEach { usuarioId ->
                        val usuario = usuarioService.getUsuario(usuarioId)
                        if (usuario != null) {
                            nombreUsuarioCache[usuarioId] =
                                "${usuario.nombre} ${usuario.apellidos}".trim()
                        }
                    }

                // Calculamos la ocupación para el día seleccionado actualmente.
                recalcularParaFecha(fecha)
            } finally {
                uiState.value = uiState.value.copy(isLoading = false)
            }
        }
    }

    /**
     * Deriva el estado de cada plaza (LIBRE / OCUPADA / BLOQUEADA_POR_TANDEM) a partir
     * de las reservas que caen en el [dia] indicado. Así una reserva del 1 de julio
     * solo afecta al 1 de julio y no aparece los demás días.
     */
    private fun recalcularParaFecha(dia: LocalDate) {
        val zona = ZoneId.systemDefault()

        val reservasDelDia = reservasActivas.filter { reserva ->
            val ts = reserva.fechaReserva ?: reserva.horaInicio
            ts != null && ts.toDate().toInstant().atZone(zona).toLocalDate() == dia
        }

        val plazasOcupadasIds = reservasDelDia
            .map { it.plazaId }
            .filter { it.isNotBlank() }
            .toSet()

        // Bloqueo unidireccional del tándem: una plaza A ocupada bloquea a la plaza B
        // indicada en su campo `plazaBloqueadaId` (así en Firebase basta con rellenar
        // ese campo en la plaza A apuntando a la B que bloquea).
        val plazasBloqueadasPorTandem = plazasBase
            .filter { it.id in plazasOcupadasIds && it.plazaBloqueadaId.isNotBlank() }
            .map { it.plazaBloqueadaId }
            .toSet()

        val ocupantePorPlaza = reservasDelDia.mapNotNull { reserva ->
            val nombre = nombreUsuarioCache[reserva.usuarioId]
            if (reserva.plazaId.isNotBlank() && !nombre.isNullOrBlank()) {
                reserva.plazaId to nombre
            } else null
        }.toMap()

        val plazasAjustadas = plazasBase.map { plaza ->
            val estado = when {
                plaza.id in plazasOcupadasIds -> EstadoPlaza.OCUPADA
                plaza.id in plazasBloqueadasPorTandem -> EstadoPlaza.BLOQUEADA_POR_TANDEM
                else -> EstadoPlaza.LIBRE
            }
            plaza.copy(estado = estado.name)
        }

        uiState.value = uiState.value.copy(
            plazas = plazasAjustadas,
            ocupantePorPlaza = ocupantePorPlaza
        )
    }

    private fun cargarVehiculos() {
        launchCatching {
            val vehiculos = vehiculoService.getVehiculos(accountService.currentUserId)
            // Preseleccionamos el primer coche registrado, si lo hay
            uiState.value = uiState.value.copy(
                vehiculos = vehiculos,
                matriculaSeleccionada = vehiculos.firstOrNull()?.matricula ?: ""
            )
        }
    }

    fun onReservarClick(plaza: Plaza) {
        if (plaza.estadoEnum != EstadoPlaza.LIBRE) {
            SnackbarManager.showMessage(AppText.plaza_no_disponible)
            return
        }
        if (matricula.isBlank()) {
            SnackbarManager.showMessage(AppText.vehiculo_no_seleccionado_error)
            return
        }
        if (!validarFechaYHora()) return

        launchCatching {
            val zona = ZoneId.systemDefault()
            val inicio = Timestamp(Date.from(fecha.atTime(horaInicio).atZone(zona).toInstant()))
            val fin = Timestamp(Date.from(fecha.atTime(horaFin).atZone(zona).toInstant()))
            val dia = Timestamp(Date.from(fecha.atStartOfDay(zona).toInstant()))

            val reserva = Reserva(
                usuarioId = accountService.currentUserId,
                plazaId = plaza.id,
                matriculaVehiculo = matricula,
                estado = EstadoReserva.AGENDADA,
                fechaReserva = dia,
                horaInicio = inicio,
                horaFin = fin
            )

            // 1. Crear la reserva en Firestore
            reservaService.crearReserva(reserva)

            // 2. Avisar y recargar: la ocupación se recalcula por día a partir de las reservas,
            //    por eso NO marcamos la plaza como OCUPADA de forma global (eso la ocuparía todos los días).
            SnackbarManager.showMessage(AppText.reserva_creada)
            cargarPlazas()
        }
    }

    /** Valida las reglas de negocio: máx. 7 días de antelación y máx. 9 horas de duración. */
    private fun validarFechaYHora(): Boolean {
        val hoy = LocalDate.now()

        if (fecha.isBefore(hoy)) {
            SnackbarManager.showMessage(AppText.fecha_pasada_error)
            return false
        }
        if (fecha.isAfter(hoy.plusDays(MAX_DIAS_ANTELACION))) {
            SnackbarManager.showMessage(AppText.fecha_max_error)
            return false
        }
        if (!horaFin.isAfter(horaInicio)) {
            SnackbarManager.showMessage(AppText.hora_invalida_error)
            return false
        }
        if (Duration.between(horaInicio, horaFin).toMinutes() > MAX_HORAS_DURACION * 60) {
            SnackbarManager.showMessage(AppText.duracion_error)
            return false
        }
        return true
    }
}
