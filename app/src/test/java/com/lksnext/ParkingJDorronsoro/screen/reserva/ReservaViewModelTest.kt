package com.lksnext.ParkingJDorronsoro.screen.reserva

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.firebase.Timestamp
import com.lksnext.ParkingJDorronsoro.common.AppText
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarManager
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarMessage
import com.lksnext.ParkingJDorronsoro.model.EstadoPlaza
import com.lksnext.ParkingJDorronsoro.model.Plaza
import com.lksnext.ParkingJDorronsoro.model.Reserva
import com.lksnext.ParkingJDorronsoro.model.Vehiculo
import com.lksnext.ParkingJDorronsoro.model.service.AccountService
import com.lksnext.ParkingJDorronsoro.model.service.LogService
import com.lksnext.ParkingJDorronsoro.model.service.NotificacionService
import com.lksnext.ParkingJDorronsoro.model.service.PlazaService
import com.lksnext.ParkingJDorronsoro.model.service.ReservaService
import com.lksnext.ParkingJDorronsoro.model.service.UsuarioService
import com.lksnext.ParkingJDorronsoro.model.service.VehiculoService
import com.lksnext.ParkingJDorronsoro.util.MainDispatcherRule
import com.lksnext.ParkingJDorronsoro.AparkauRoutes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class ReservaViewModelTest {

    // ─── Rules ────────────────────────────────────────────────────────────────

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ─── Mocks ────────────────────────────────────────────────────────────────

    private val mockAccountService = mockk<AccountService>(relaxed = true)
    private val mockPlazaService = mockk<PlazaService>(relaxed = true)
    private val mockReservaService = mockk<ReservaService>(relaxed = true)
    private val mockVehiculoService = mockk<VehiculoService>(relaxed = true)
    private val mockUsuarioService = mockk<UsuarioService>(relaxed = true)
    private val mockNotificacionService = mockk<NotificacionService>(relaxed = true)
    private val mockLogService = mockk<LogService>(relaxed = true)

    private lateinit var viewModel: ReservaViewModel

    // ─── Setup / Teardown ─────────────────────────────────────────────────────

    @Before
    fun setUp() {
        every { mockAccountService.currentUserId } returns "uid-test"
        coEvery { mockPlazaService.getTodasLasPlazas() } returns emptyList()
        coEvery { mockReservaService.getTodasLasReservasActivas() } returns emptyList()
        coEvery { mockVehiculoService.getVehiculos(any()) } returns emptyList()
        coEvery { mockUsuarioService.getUsuario(any()) } returns null

        viewModel = crearViewModel()
    }

    @After
    fun tearDown() {
        unmockkAll()
        SnackbarManager.clearSnackbarState()
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun crearViewModel() = ReservaViewModel(
        mockAccountService, mockPlazaService, mockReservaService,
        mockVehiculoService, mockUsuarioService, mockNotificacionService, mockLogService
    )

    /** Plaza LIBRE lista para reservar. */
    private fun plazaLibre(id: String = "1A") = Plaza(id = id, estado = "LIBRE")

    /** Plaza OCUPADA (no disponible). */
    private fun plazaOcupada(id: String = "1A") = Plaza(id = id, estado = "OCUPADA")

    /** Plaza BLOQUEADA por tándem. */
    private fun plazaBloqueada(id: String = "1A") = Plaza(id = id, estado = "BLOQUEADA_POR_TANDEM")

    /**
     * Timestamp cuya fecha al convertir con toDate() coincide con [fecha].
     * Necesario para que recalcularParaFecha() incluya la reserva en el día correcto.
     */
    private fun timestampDe(fecha: LocalDate): Timestamp {
        val date = Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant())
        return mockk<Timestamp>(relaxed = true).also { every { it.toDate() } returns date }
    }

    /** Reserva del usuario actual con fecha = HOY. */
    private fun reservaHoy(plazaId: String = "1A", usuarioId: String = "uid-test"): Reserva {
        return Reserva(
            id = "r-test",
            usuarioId = usuarioId,
            plazaId = plazaId,
            fechaReserva = timestampDe(LocalDate.now())
        )
    }

    // ─── Estado inicial (init) ────────────────────────────────────────────────

    @Test
    fun `estado inicial tiene fecha de hoy`() = runTest {
        advanceUntilIdle()
        assertEquals(LocalDate.now(), viewModel.uiState.value.fecha)
    }

    @Test
    fun `estado inicial tiene horaInicio las 8h00`() = runTest {
        advanceUntilIdle()
        assertEquals(LocalTime.of(8, 0), viewModel.uiState.value.horaInicio)
    }

    @Test
    fun `estado inicial tiene horaFin las 17h00`() = runTest {
        advanceUntilIdle()
        assertEquals(LocalTime.of(17, 0), viewModel.uiState.value.horaFin)
    }

    @Test
    fun `init isLoading es false cuando cargarPlazas termina`() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── cargarVehiculos (en init) ────────────────────────────────────────────

    @Test
    fun `init preselecciona la matricula del primer vehiculo`() = runTest {
        coEvery { mockVehiculoService.getVehiculos(any()) } returns
                listOf(Vehiculo(matricula = "1234-ABC"), Vehiculo(matricula = "5678-XYZ"))

        val vm = crearViewModel()
        advanceUntilIdle()

        assertEquals("1234-ABC", vm.uiState.value.matriculaSeleccionada)
    }

    @Test
    fun `init con lista de vehiculos vacia deja matricula vacia`() = runTest {
        advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.matriculaSeleccionada)
    }

    @Test
    fun `init carga la lista de vehiculos en el estado`() = runTest {
        val vehiculos = listOf(Vehiculo(matricula = "1234-ABC"), Vehiculo(matricula = "5678-XYZ"))
        coEvery { mockVehiculoService.getVehiculos(any()) } returns vehiculos

        val vm = crearViewModel()
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.vehiculos.size)
    }

    // ─── Actualización de estado ───────────────────────────────────────────────

    @Test
    fun `onVehiculoSelected actualiza la matricula en el estado`() = runTest {
        advanceUntilIdle()
        viewModel.onVehiculoSelected("9999-ZZZ")
        assertEquals("9999-ZZZ", viewModel.uiState.value.matriculaSeleccionada)
    }

    @Test
    fun `onFechaChange actualiza la fecha en el estado`() = runTest {
        advanceUntilIdle()
        val manana = LocalDate.now().plusDays(1)
        viewModel.onFechaChange(manana)
        assertEquals(manana, viewModel.uiState.value.fecha)
    }

    @Test
    fun `onHoraInicioChange actualiza la hora de inicio`() = runTest {
        advanceUntilIdle()
        viewModel.onHoraInicioChange(LocalTime.of(9, 30))
        assertEquals(LocalTime.of(9, 30), viewModel.uiState.value.horaInicio)
    }

    @Test
    fun `onHoraFinChange actualiza la hora de fin`() = runTest {
        advanceUntilIdle()
        viewModel.onHoraFinChange(LocalTime.of(18, 0))
        assertEquals(LocalTime.of(18, 0), viewModel.uiState.value.horaFin)
    }

    // ─── recalcularParaFecha ───────────────────────────────────────────────────

    @Test
    fun `cargarPlazas marca como OCUPADA la plaza que tiene reserva hoy`() = runTest {
        coEvery { mockPlazaService.getTodasLasPlazas() } returns listOf(plazaLibre("1A"))
        coEvery { mockReservaService.getTodasLasReservasActivas() } returns
                listOf(reservaHoy("1A"))

        val vm = crearViewModel()
        advanceUntilIdle()

        val estadoPlaza = vm.uiState.value.plazas.first { it.id == "1A" }.estadoEnum
        assertEquals(EstadoPlaza.OCUPADA, estadoPlaza)
    }

    @Test
    fun `cargarPlazas deja LIBRE la plaza que no tiene reservas hoy`() = runTest {
        coEvery { mockPlazaService.getTodasLasPlazas() } returns listOf(plazaLibre("1A"))
        coEvery { mockReservaService.getTodasLasReservasActivas() } returns emptyList()

        val vm = crearViewModel()
        advanceUntilIdle()

        val estadoPlaza = vm.uiState.value.plazas.first { it.id == "1A" }.estadoEnum
        assertEquals(EstadoPlaza.LIBRE, estadoPlaza)
    }

    @Test
    fun `cargarPlazas marca BLOQUEADA_POR_TANDEM la plaza trasera cuando su bloqueadora esta ocupada`() =
        runTest {
            // Plaza "1A" (TANDEM) bloquea a plaza "1B" cuando está ocupada
            val plazaA = Plaza(id = "1A", tipo = "TANDEM", estado = "LIBRE", plazaBloqueadaId = "1B")
            val plazaB = Plaza(id = "1B", estado = "LIBRE")

            coEvery { mockPlazaService.getTodasLasPlazas() } returns listOf(plazaA, plazaB)
            coEvery { mockReservaService.getTodasLasReservasActivas() } returns
                    listOf(reservaHoy("1A")) // alguien ocupa 1A hoy

            val vm = crearViewModel()
            advanceUntilIdle()

            val estadoB = vm.uiState.value.plazas.first { it.id == "1B" }.estadoEnum
            assertEquals(EstadoPlaza.BLOQUEADA_POR_TANDEM, estadoB)
        }

    @Test
    fun `onFechaChange recalcula las plazas para la nueva fecha`() = runTest {
        val manana = LocalDate.now().plusDays(1)
        val plazaA = plazaLibre("1A")
        // Reserva de mañana (no de hoy)
        val reservaManana = Reserva(
            id = "r2",
            usuarioId = "otro",
            plazaId = "1A",
            fechaReserva = timestampDe(manana)
        )

        coEvery { mockPlazaService.getTodasLasPlazas() } returns listOf(plazaA)
        coEvery { mockReservaService.getTodasLasReservasActivas() } returns listOf(reservaManana)

        val vm = crearViewModel()
        advanceUntilIdle()

        // Hoy la plaza debe estar LIBRE (la reserva es de mañana)
        assertEquals(EstadoPlaza.LIBRE, vm.uiState.value.plazas.first().estadoEnum)

        // Al cambiar a mañana, la plaza pasa a OCUPADA
        vm.onFechaChange(manana)
        assertEquals(EstadoPlaza.OCUPADA, vm.uiState.value.plazas.first().estadoEnum)
    }

    // ─── onVolverClick ────────────────────────────────────────────────────────

    @Test
    fun `onVolverClick navega a HOME haciendo pop de HOME para recargarlo`() = runTest {
        advanceUntilIdle()
        var destino = ""
        var popUp = ""

        viewModel.onVolverClick { dest, pop ->
            destino = dest
            popUp = pop
        }

        assertEquals(AparkauRoutes.HOME_SCREEN, destino)
        assertEquals(AparkauRoutes.HOME_SCREEN, popUp)
    }

    // ─── onReservarClick: validaciones ────────────────────────────────────────

    @Test
    fun `onReservarClick con plaza OCUPADA muestra error de plaza no disponible`() = runTest {
        advanceUntilIdle()
        viewModel.onReservarClick(plazaOcupada())

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.plaza_no_disponible, (mensaje as SnackbarMessage.ResourceSnackbar).message)
        coVerify(exactly = 0) { mockReservaService.crearReserva(any()) }
    }

    @Test
    fun `onReservarClick con plaza BLOQUEADA_POR_TANDEM muestra error de plaza no disponible`() =
        runTest {
            advanceUntilIdle()
            viewModel.onReservarClick(plazaBloqueada())

            val mensaje = SnackbarManager.snackbarMessages.value
            assertNotNull(mensaje)
            assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
            assertEquals(
                AppText.plaza_no_disponible,
                (mensaje as SnackbarMessage.ResourceSnackbar).message
            )
            coVerify(exactly = 0) { mockReservaService.crearReserva(any()) }
        }

    @Test
    fun `onReservarClick con matricula vacia muestra error de vehiculo no seleccionado`() =
        runTest {
            advanceUntilIdle()
            // No seleccionamos vehículo → matricula = ""
            viewModel.onReservarClick(plazaLibre())

            val mensaje = SnackbarManager.snackbarMessages.value
            assertNotNull(mensaje)
            assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
            assertEquals(
                AppText.vehiculo_no_seleccionado_error,
                (mensaje as SnackbarMessage.ResourceSnackbar).message
            )
            coVerify(exactly = 0) { mockReservaService.crearReserva(any()) }
        }

    @Test
    fun `onReservarClick con fecha pasada muestra error de fecha pasada`() = runTest {
        advanceUntilIdle()
        viewModel.onVehiculoSelected("1234-ABC")
        viewModel.onFechaChange(LocalDate.now().minusDays(1)) // ayer

        viewModel.onReservarClick(plazaLibre())

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.fecha_pasada_error, (mensaje as SnackbarMessage.ResourceSnackbar).message)
        coVerify(exactly = 0) { mockReservaService.crearReserva(any()) }
    }

    @Test
    fun `onReservarClick con fecha a mas de 7 dias muestra error de fecha maxima`() = runTest {
        advanceUntilIdle()
        viewModel.onVehiculoSelected("1234-ABC")
        viewModel.onFechaChange(LocalDate.now().plusDays(8)) // 8 días → supera el límite

        viewModel.onReservarClick(plazaLibre())

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.fecha_max_error, (mensaje as SnackbarMessage.ResourceSnackbar).message)
        coVerify(exactly = 0) { mockReservaService.crearReserva(any()) }
    }

    @Test
    fun `onReservarClick con horaFin igual a horaInicio muestra error de hora invalida`() =
        runTest {
            advanceUntilIdle()
            viewModel.onVehiculoSelected("1234-ABC")
            viewModel.onHoraInicioChange(LocalTime.of(10, 0))
            viewModel.onHoraFinChange(LocalTime.of(10, 0)) // igual → no es después

            viewModel.onReservarClick(plazaLibre())

            val mensaje = SnackbarManager.snackbarMessages.value
            assertNotNull(mensaje)
            assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
            assertEquals(
                AppText.hora_invalida_error,
                (mensaje as SnackbarMessage.ResourceSnackbar).message
            )
            coVerify(exactly = 0) { mockReservaService.crearReserva(any()) }
        }

    @Test
    fun `onReservarClick con horaFin antes de horaInicio muestra error de hora invalida`() =
        runTest {
            advanceUntilIdle()
            viewModel.onVehiculoSelected("1234-ABC")
            viewModel.onHoraInicioChange(LocalTime.of(17, 0))
            viewModel.onHoraFinChange(LocalTime.of(8, 0)) // fin antes que inicio

            viewModel.onReservarClick(plazaLibre())

            val mensaje = SnackbarManager.snackbarMessages.value
            assertNotNull(mensaje)
            assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
            assertEquals(
                AppText.hora_invalida_error,
                (mensaje as SnackbarMessage.ResourceSnackbar).message
            )
        }

    @Test
    fun `onReservarClick con duracion superior a 9h muestra error de duracion`() = runTest {
        advanceUntilIdle()
        viewModel.onVehiculoSelected("1234-ABC")
        viewModel.onHoraInicioChange(LocalTime.of(8, 0))
        viewModel.onHoraFinChange(LocalTime.of(17, 1)) // 9h 1min > 9h

        viewModel.onReservarClick(plazaLibre())

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.duracion_error, (mensaje as SnackbarMessage.ResourceSnackbar).message)
        coVerify(exactly = 0) { mockReservaService.crearReserva(any()) }
    }

    @Test
    fun `onReservarClick con duracion exactamente 9h no muestra error de duracion`() = runTest {
        advanceUntilIdle()
        coEvery { mockReservaService.crearReserva(any()) } returns "nueva-id"

        viewModel.onVehiculoSelected("1234-ABC")
        viewModel.onHoraInicioChange(LocalTime.of(8, 0))
        viewModel.onHoraFinChange(LocalTime.of(17, 0)) // exactamente 9h → válido

        viewModel.onReservarClick(plazaLibre())
        advanceUntilIdle()

        // No debe haber error de duración
        val mensaje = SnackbarManager.snackbarMessages.value
        if (mensaje is SnackbarMessage.ResourceSnackbar) {
            assertTrue(
                "No deberia mostrar error de duracion",
                mensaje.message != AppText.duracion_error
            )
        }
    }

    @Test
    fun `onReservarClick cuando el usuario ya tiene reserva ese dia muestra error`() = runTest {
        // El usuario ya tiene una reserva para hoy
        coEvery { mockReservaService.getTodasLasReservasActivas() } returns
                listOf(reservaHoy("2A", "uid-test"))

        val vm = crearViewModel()
        advanceUntilIdle()

        vm.onVehiculoSelected("1234-ABC")
        // fecha = LocalDate.now() (por defecto) → mismo día que la reserva existente
        vm.onReservarClick(plazaLibre("1A")) // intenta reservar otra plaza

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(
            AppText.ya_tiene_reserva_dia_error,
            (mensaje as SnackbarMessage.ResourceSnackbar).message
        )
        coVerify(exactly = 0) { mockReservaService.crearReserva(any()) }
    }

    // ─── onReservarClick: flujo exitoso ───────────────────────────────────────

    @Test
    fun `onReservarClick valido llama a crearReserva con los datos correctos`() = runTest {
        coEvery { mockReservaService.crearReserva(any()) } returns "nueva-reserva-id"

        val vm = crearViewModel()
        advanceUntilIdle()

        vm.onVehiculoSelected("1234-ABC")
        vm.onHoraInicioChange(LocalTime.of(8, 0))
        vm.onHoraFinChange(LocalTime.of(17, 0))
        vm.onReservarClick(plazaLibre("1A"))
        advanceUntilIdle()

        coVerify {
            mockReservaService.crearReserva(
                match { reserva ->
                    reserva.plazaId == "1A" &&
                        reserva.matriculaVehiculo == "1234-ABC" &&
                        reserva.usuarioId == "uid-test"
                }
            )
        }
    }

    @Test
    fun `onReservarClick valido programa los recordatorios de notificacion`() = runTest {
        coEvery { mockReservaService.crearReserva(any()) } returns "nueva-reserva-id"

        val vm = crearViewModel()
        advanceUntilIdle()

        vm.onVehiculoSelected("1234-ABC")
        vm.onHoraInicioChange(LocalTime.of(8, 0))
        vm.onHoraFinChange(LocalTime.of(17, 0))
        vm.onReservarClick(plazaLibre("1A"))
        advanceUntilIdle()

        verify {
            mockNotificacionService.programarRecordatorios(
                reservaId = "nueva-reserva-id",
                horaInicioMs = any(),
                horaFinMs = any(),
                plazaId = "1A"
            )
        }
    }

    @Test
    fun `onReservarClick valido muestra snackbar de reserva creada`() = runTest {
        coEvery { mockReservaService.crearReserva(any()) } returns "nueva-reserva-id"

        val vm = crearViewModel()
        advanceUntilIdle()
        SnackbarManager.clearSnackbarState()

        vm.onVehiculoSelected("1234-ABC")
        vm.onHoraInicioChange(LocalTime.of(8, 0))
        vm.onHoraFinChange(LocalTime.of(17, 0))
        vm.onReservarClick(plazaLibre("1A"))
        advanceUntilIdle()

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.reserva_creada, (mensaje as SnackbarMessage.ResourceSnackbar).message)
    }

    @Test
    fun `onReservarClick valido no muestra ningun error de validacion`() = runTest {
        coEvery { mockReservaService.crearReserva(any()) } returns "nueva-reserva-id"

        val vm = crearViewModel()
        advanceUntilIdle()

        vm.onVehiculoSelected("1234-ABC")
        vm.onHoraInicioChange(LocalTime.of(8, 0))
        vm.onHoraFinChange(LocalTime.of(17, 0))
        vm.onReservarClick(plazaLibre("1A"))
        advanceUntilIdle()

        val mensaje = SnackbarManager.snackbarMessages.value
        // Solo debe existir el snackbar de éxito, nunca uno de error
        if (mensaje is SnackbarMessage.ResourceSnackbar) {
            assertEquals(AppText.reserva_creada, mensaje.message)
        }
    }
}
