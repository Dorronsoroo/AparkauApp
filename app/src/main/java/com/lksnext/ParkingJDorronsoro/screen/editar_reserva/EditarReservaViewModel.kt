package com.lksnext.ParkingJDorronsoro.screen.editar_reserva

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import com.lksnext.ParkingJDorronsoro.AparkauRoutes
import com.lksnext.ParkingJDorronsoro.MakeItSoViewModel
import com.lksnext.ParkingJDorronsoro.common.AppText
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarManager
import com.lksnext.ParkingJDorronsoro.model.EstadoPlaza
import com.lksnext.ParkingJDorronsoro.model.Plaza
import com.lksnext.ParkingJDorronsoro.model.Reserva
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
class EditarReservaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountService: AccountService,
    private val plazaService: PlazaService,
    private val reservaService: ReservaService,
    private val vehiculoService: VehiculoService,
    private val usuarioService: UsuarioService,
    logService: LogService
) : MakeItSoViewModel(logService) {

    private val reservaId: String = checkNotNull(savedStateHandle["reservaId"])

    var uiState = mutableStateOf(EditarReservaUiState(reservaId = reservaId))
        private set

    private val fecha get() = uiState.value.fecha
    private val horaInicio get() = uiState.value.horaInicio
    private val horaFin get() = uiState.value.horaFin

    // Datos base cargados de Firestore.
    private var plazasBase: List<Plaza> = emptyList()
    private var reservasActivas: List<Reserva> = emptyList()
    private val nombreUsuarioCache = mutableMapOf<String, String>()

    companion object {
        private const val MAX_DIAS_ANTELACION = 7L
        private const val MAX_HORAS_DURACION = 9L
    }

    init {
        cargarDatos()
    }

    fun onVehiculoSelected(newValue: String) {
        uiState.value = uiState.value.copy(matriculaSeleccionada = newValue)
    }

    fun onFechaChange(newValue: LocalDate) {
        uiState.value = uiState.value.copy(fecha = newValue)
        recalcularParaFecha(newValue)
    }

    fun onHoraInicioChange(newValue: LocalTime) {
        uiState.value = uiState.value.copy(horaInicio = newValue)
    }

    fun onHoraFinChange(newValue: LocalTime) {
        uiState.value = uiState.value.copy(horaFin = newValue)
    }

    fun onPlazaSeleccionadaChange(plazaId: String) {
        uiState.value = uiState.value.copy(plazaSeleccionadaId = plazaId)
    }

    fun onVolverClick(openAndPopUp: (String, String) -> Unit) {
        openAndPopUp(AparkauRoutes.HOME_SCREEN, AparkauRoutes.HOME_SCREEN)
    }

    private fun cargarDatos() {
        uiState.value = uiState.value.copy(isLoading = true)
        launchCatching {
            try {
                // 1. Cargamos la reserva existente para pre-rellenar el formulario
                val reserva = reservaService.getReserva(reservaId)
                if (reserva == null) {
                    SnackbarManager.showMessage(AppText.cargando_reserva_error)
                    return@launchCatching
                }

                val zona = ZoneId.systemDefault()
                val fechaReserva = (reserva.fechaReserva ?: reserva.horaInicio)
                    ?.toDate()?.toInstant()?.atZone(zona)?.toLocalDate()
                    ?: LocalDate.now()
                val horaInicioLocal = reserva.horaInicio
                    ?.toDate()?.toInstant()?.atZone(zona)?.toLocalTime()
                    ?: LocalTime.of(8, 0)
                val horaFinLocal = reserva.horaFin
                    ?.toDate()?.toInstant()?.atZone(zona)?.toLocalTime()
                    ?: LocalTime.of(17, 0)

                uiState.value = uiState.value.copy(
                    plazaSeleccionadaId = reserva.plazaId,
                    matriculaSeleccionada = reserva.matriculaVehiculo,
                    fecha = fechaReserva,
                    horaInicio = horaInicioLocal,
                    horaFin = horaFinLocal
                )

                // 2. Cargamos plazas y todas las reservas activas
                plazasBase = plazaService.getTodasLasPlazas()
                    .sortedWith(
                        compareBy(
                            { it.id.takeWhile { c -> c.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE },
                            { it.id }
                        )
                    )

                reservasActivas = reservaService.getTodasLasReservasActivas()

                reservasActivas
                    .map { it.usuarioId }
                    .filter { it.isNotBlank() && !nombreUsuarioCache.containsKey(it) }
                    .distinct()
                    .forEach { uid ->
                        val usuario = usuarioService.getUsuario(uid)
                        if (usuario != null) {
                            nombreUsuarioCache[uid] = "${usuario.nombre} ${usuario.apellidos}".trim()
                        }
                    }

                // 3. Cargamos vehículos del usuario
                val vehiculos = vehiculoService.getVehiculos(accountService.currentUserId)
                uiState.value = uiState.value.copy(vehiculos = vehiculos)

                // 4. Calculamos disponibilidad para la fecha de la reserva
                recalcularParaFecha(fechaReserva)
            } finally {
                uiState.value = uiState.value.copy(isLoading = false)
            }
        }
    }

    /**
     * Deriva el estado de cada plaza para [dia], excluyendo la reserva que se está
     * editando para que su plaza aparezca como LIBRE.
     */
    private fun recalcularParaFecha(dia: LocalDate) {
        val zona = ZoneId.systemDefault()

        val reservasDelDia = reservasActivas.filter { reserva ->
            // Excluimos la reserva que se está editando
            if (reserva.id == reservaId) return@filter false
            val ts = reserva.fechaReserva ?: reserva.horaInicio
            ts != null && ts.toDate().toInstant().atZone(zona).toLocalDate() == dia
        }

        val plazasOcupadasIds = reservasDelDia.map { it.plazaId }.filter { it.isNotBlank() }.toSet()

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

    fun onActualizarClick(openAndPopUp: (String, String) -> Unit) {
        val plazaId = uiState.value.plazaSeleccionadaId
        if (plazaId.isBlank()) {
            SnackbarManager.showMessage(AppText.plaza_no_disponible)
            return
        }
        if (uiState.value.matriculaSeleccionada.isBlank()) {
            SnackbarManager.showMessage(AppText.vehiculo_no_seleccionado_error)
            return
        }
        if (!validarFechaYHora()) return

        // Comprobar que el usuario no tenga ya OTRA reserva ese día (distinta a la que editamos).
        val uid = accountService.currentUserId
        val zona = ZoneId.systemDefault()
        val yaReservado = reservasActivas.any { r ->
            r.id != reservaId &&
                r.usuarioId == uid &&
                (r.fechaReserva ?: r.horaInicio)
                    ?.toDate()?.toInstant()?.atZone(zona)?.toLocalDate() == fecha
        }
        if (yaReservado) {
            SnackbarManager.showMessage(AppText.ya_tiene_reserva_dia_error)
            return
        }

        launchCatching {
            val zona = ZoneId.systemDefault()
            val inicio = Timestamp(Date.from(fecha.atTime(horaInicio).atZone(zona).toInstant()))
            val fin = Timestamp(Date.from(fecha.atTime(horaFin).atZone(zona).toInstant()))
            val dia = Timestamp(Date.from(fecha.atStartOfDay(zona).toInstant()))

            reservaService.actualizarReserva(
                reservaId = reservaId,
                plazaId = plazaId,
                matriculaVehiculo = uiState.value.matriculaSeleccionada,
                fechaReserva = dia,
                horaInicio = inicio,
                horaFin = fin
            )

            SnackbarManager.showMessage(AppText.reserva_actualizada)
            openAndPopUp(AparkauRoutes.HOME_SCREEN, AparkauRoutes.HOME_SCREEN)
        }
    }

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

