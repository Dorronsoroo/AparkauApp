package com.lksnext.ParkingJDorronsoro.screen.editar_reserva

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.google.firebase.Timestamp
import com.lksnext.ParkingJDorronsoro.AparkauRoutes
import com.lksnext.ParkingJDorronsoro.common.AppText
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarManager
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarMessage
import com.lksnext.ParkingJDorronsoro.model.EstadoPlaza
import com.lksnext.ParkingJDorronsoro.model.Plaza
import com.lksnext.ParkingJDorronsoro.model.Reserva
import com.lksnext.ParkingJDorronsoro.model.service.AccountService
import com.lksnext.ParkingJDorronsoro.model.service.LogService
import com.lksnext.ParkingJDorronsoro.model.service.NotificacionService
import com.lksnext.ParkingJDorronsoro.model.service.PlazaService
import com.lksnext.ParkingJDorronsoro.model.service.ReservaService
import com.lksnext.ParkingJDorronsoro.model.service.UsuarioService
import com.lksnext.ParkingJDorronsoro.model.service.VehiculoService
import com.lksnext.ParkingJDorronsoro.util.MainDispatcherRule
import io.mockk.clearMocks
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class EditarReservaViewModelTest {

    // ─── Rules ────────────────────────────────────────────────────────────────

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ─── Mocks ────────────────────────────────────────────────────────────────

    private val mockSavedStateHandle = mockk<SavedStateHandle>()
    private val mockAccountService = mockk<AccountService>(relaxed = true)
    private val mockPlazaService = mockk<PlazaService>(relaxed = true)
    private val mockReservaService = mockk<ReservaService>(relaxed = true)
    private val mockVehiculoService = mockk<VehiculoService>(relaxed = true)
    private val mockUsuarioService = mockk<UsuarioService>(relaxed = true)
    private val mockNotificacionService = mockk<NotificacionService>(relaxed = true)
    private val mockLogService = mockk<LogService>(relaxed = true)

    private lateinit var viewModel: EditarReservaViewModel

    // ─── Setup / Teardown ─────────────────────────────────────────────────────

    @Before
    fun setUp() {
        every { mockSavedStateHandle.get<String>("reservaId") } returns "r-editar"
        every { mockAccountService.currentUserId } returns "uid-test"
        coEvery { mockReservaService.getReserva(any()) } returns reservaBase()
        coEvery { mockReservaService.getTodasLasReservasActivas() } returns emptyList()
        coEvery { mockPlazaService.getTodasLasPlazas() } returns emptyList()
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

    private fun crearViewModel() = EditarReservaViewModel(
        mockSavedStateHandle, mockAccountService, mockPlazaService, mockReservaService,
        mockVehiculoService, mockUsuarioService, mockNotificacionService, mockLogService
    )

    /**
     * Crea un Timestamp mockeado cuyo toDate() devuelve la fecha+hora indicada.
     * Necesario para los conversores LocalDate/LocalTime dentro del ViewModel.
     */
    private fun timestampDe(fecha: LocalDate, hora: LocalTime = LocalTime.MIDNIGHT): Timestamp =
        mockk<Timestamp>(relaxed = true).also {
            every { it.toDate() } returns
                    Date.from(fecha.atTime(hora).atZone(ZoneId.systemDefault()).toInstant())
        }

    /** Reserva de ejemplo que usa el ViewModel al cargar. */
    private fun reservaBase(
        id: String = "r-editar",
        plazaId: String = "1A",
        matricula: String = "1234-ABC",
        fecha: LocalDate = LocalDate.now(),
        inicio: LocalTime = LocalTime.of(9, 0),
        fin: LocalTime = LocalTime.of(14, 0)
    ) = Reserva(
        id = id,
        usuarioId = "uid-test",
        plazaId = plazaId,
        matriculaVehiculo = matricula,
        fechaReserva = timestampDe(fecha),
        horaInicio = timestampDe(fecha, inicio),
        horaFin = timestampDe(fecha, fin)
    )

    // ─── cargarDatos: pre-relleno del formulario ──────────────────────────────

    @Test
    fun `cargarDatos pre-rellena la plaza seleccionada con la de la reserva`() = runTest {
        advanceUntilIdle()
        assertEquals("1A", viewModel.uiState.value.plazaSeleccionadaId)
    }

    @Test
    fun `cargarDatos pre-rellena la matricula con la de la reserva`() = runTest {
        advanceUntilIdle()
        assertEquals("1234-ABC", viewModel.uiState.value.matriculaSeleccionada)
    }

    @Test
    fun `cargarDatos pre-rellena la fecha con la de la reserva`() = runTest {
        val ayer = LocalDate.now().minusDays(1) // cualquier fecha no-default
        coEvery { mockReservaService.getReserva(any()) } returns reservaBase(fecha = ayer)

        val vm = crearViewModel()
        advanceUntilIdle()

        assertEquals(ayer, vm.uiState.value.fecha)
    }

    @Test
    fun `cargarDatos pre-rellena las horas de inicio y fin`() = runTest {
        advanceUntilIdle()
        assertEquals(LocalTime.of(9, 0), viewModel.uiState.value.horaInicio)
        assertEquals(LocalTime.of(14, 0), viewModel.uiState.value.horaFin)
    }

    @Test
    fun `cargarDatos muestra error y no continua si la reserva no existe`() = runTest {
        coEvery { mockReservaService.getReserva(any()) } returns null

        // El setUp ya creó un ViewModel que llamó a getTodasLasPlazas().
        // Limpiamos el historial de llamadas (sin borrar los stubs) para
        // que el coVerify(exactly = 0) solo cuente las llamadas de ESTE test.
        clearMocks(mockPlazaService, answers = false)

        crearViewModel()
        advanceUntilIdle()

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(
            AppText.cargando_reserva_error,
            (mensaje as SnackbarMessage.ResourceSnackbar).message
        )
        // Sin reserva, cargarDatos hace return@launchCatching → no se cargan plazas
        coVerify(exactly = 0) { mockPlazaService.getTodasLasPlazas() }
    }

    @Test
    fun `isLoading es false cuando cargarDatos termina`() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── recalcularParaFecha: la plaza editada queda LIBRE ────────────────────
    // Este comportamiento es EXCLUSIVO de EditarReserva y no existe en ReservaViewModel.

    @Test
    fun `recalcularParaFecha deja la plaza de la reserva editada como LIBRE`() = runTest {
        // La reserva que estamos editando ocupa la plaza "1A" hoy
        val reservaEditada = reservaBase(id = "r-editar", plazaId = "1A")

        coEvery { mockReservaService.getReserva("r-editar") } returns reservaEditada
        coEvery { mockReservaService.getTodasLasReservasActivas() } returns listOf(reservaEditada)
        coEvery { mockPlazaService.getTodasLasPlazas() } returns
                listOf(Plaza(id = "1A", estado = "LIBRE"))

        val vm = crearViewModel()
        advanceUntilIdle()

        // La plaza 1A debe estar LIBRE aunque esté en reservasActivas,
        // porque su id coincide con reservaId y se excluye del cálculo.
        val estadoPlaza = vm.uiState.value.plazas.first { it.id == "1A" }.estadoEnum
        assertEquals(EstadoPlaza.LIBRE, estadoPlaza)
    }

    @Test
    fun `recalcularParaFecha marca OCUPADA la plaza de OTRA reserva del mismo dia`() = runTest {
        val reservaOtra = reservaBase(id = "r-otro", plazaId = "2B", matricula = "X")

        coEvery { mockReservaService.getTodasLasReservasActivas() } returns listOf(reservaOtra)
        coEvery { mockPlazaService.getTodasLasPlazas() } returns
                listOf(Plaza(id = "2B", estado = "LIBRE"))

        val vm = crearViewModel()
        advanceUntilIdle()

        val estadoPlaza = vm.uiState.value.plazas.first { it.id == "2B" }.estadoEnum
        assertEquals(EstadoPlaza.OCUPADA, estadoPlaza)
    }

    // ─── onActualizarClick: validaciones ─────────────────────────────────────

    @Test
    fun `onActualizarClick con plazaSeleccionadaId vacia muestra error`() = runTest {
        advanceUntilIdle()
        // Vaciamos la plaza seleccionada
        viewModel.onPlazaSeleccionadaChange("")

        viewModel.onActualizarClick { _, _ -> }

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(
            AppText.plaza_no_disponible,
            (mensaje as SnackbarMessage.ResourceSnackbar).message
        )
        coVerify(exactly = 0) { mockReservaService.actualizarReserva(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `onActualizarClick con matricula vacia muestra error`() = runTest {
        advanceUntilIdle()
        viewModel.onVehiculoSelected("") // vaciar matrícula

        viewModel.onActualizarClick { _, _ -> }

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(
            AppText.vehiculo_no_seleccionado_error,
            (mensaje as SnackbarMessage.ResourceSnackbar).message
        )
    }

    @Test
    fun `onActualizarClick con fecha pasada muestra error de fecha`() = runTest {
        advanceUntilIdle()
        viewModel.onFechaChange(LocalDate.now().minusDays(1))

        viewModel.onActualizarClick { _, _ -> }

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.fecha_pasada_error, (mensaje as SnackbarMessage.ResourceSnackbar).message)
    }

    @Test
    fun `onActualizarClick permite editar la reserva al mismo dia sin error de duplicado`() =
        runTest {
            // La reserva que editamos es "r-editar" y es de hoy.
            // Al verificar duplicados, se excluye a sí misma → no debe dar error.
            coEvery { mockReservaService.getTodasLasReservasActivas() } returns
                    listOf(reservaBase(id = "r-editar")) // misma reserva, mismo día

            val vm = crearViewModel()
            advanceUntilIdle()

            coEvery { mockReservaService.actualizarReserva(any(), any(), any(), any(), any(), any()) } returns Unit

            vm.onActualizarClick { _, _ -> }
            advanceUntilIdle()

            // No debe aparecer el error de "ya tiene reserva ese día"
            val mensaje = SnackbarManager.snackbarMessages.value
            if (mensaje is SnackbarMessage.ResourceSnackbar) {
                assertTrue(
                    "No deberia mostrar error de duplicado",
                    mensaje.message != AppText.ya_tiene_reserva_dia_error
                )
            }
        }

    @Test
    fun `onActualizarClick con OTRA reserva el mismo dia muestra error de duplicado`() = runTest {
        // Hay una reserva DISTINTA del mismo usuario ese mismo día
        val otraReserva = reservaBase(id = "r-otra", plazaId = "2B")
            .copy(fechaReserva = timestampDe(LocalDate.now()))

        coEvery { mockReservaService.getTodasLasReservasActivas() } returns
                listOf(reservaBase(id = "r-editar"), otraReserva)

        val vm = crearViewModel()
        advanceUntilIdle()

        vm.onActualizarClick { _, _ -> }

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(
            AppText.ya_tiene_reserva_dia_error,
            (mensaje as SnackbarMessage.ResourceSnackbar).message
        )
    }

    // ─── onActualizarClick: flujo exitoso ─────────────────────────────────────

    @Test
    fun `onActualizarClick valido llama a actualizarReserva con el id correcto`() = runTest {
        coEvery { mockReservaService.actualizarReserva(any(), any(), any(), any(), any(), any()) } returns Unit

        val vm = crearViewModel()
        advanceUntilIdle()

        vm.onActualizarClick { _, _ -> }
        advanceUntilIdle()

        coVerify {
            mockReservaService.actualizarReserva(
                reservaId = "r-editar",
                plazaId = "1A",
                matriculaVehiculo = "1234-ABC",
                fechaReserva = any(),
                horaInicio = any(),
                horaFin = any()
            )
        }
    }

    @Test
    fun `onActualizarClick valido reprograma los recordatorios`() = runTest {
        coEvery { mockReservaService.actualizarReserva(any(), any(), any(), any(), any(), any()) } returns Unit

        val vm = crearViewModel()
        advanceUntilIdle()

        vm.onActualizarClick { _, _ -> }
        advanceUntilIdle()

        verify {
            mockNotificacionService.programarRecordatorios(
                reservaId = "r-editar",
                horaInicioMs = any(),
                horaFinMs = any(),
                plazaId = "1A"
            )
        }
    }

    @Test
    fun `onActualizarClick valido muestra snackbar de reserva actualizada y navega a HOME`() =
        runTest {
            coEvery { mockReservaService.actualizarReserva(any(), any(), any(), any(), any(), any()) } returns Unit

            var destino = ""
            var popUp = ""
            val vm = crearViewModel()
            advanceUntilIdle()

            vm.onActualizarClick { dest, pop ->
                destino = dest
                popUp = pop
            }
            advanceUntilIdle()

            val mensaje = SnackbarManager.snackbarMessages.value
            assertNotNull(mensaje)
            assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
            assertEquals(AppText.reserva_actualizada, (mensaje as SnackbarMessage.ResourceSnackbar).message)
            assertEquals(AparkauRoutes.HOME_SCREEN, destino)
            assertEquals(AparkauRoutes.HOME_SCREEN, popUp)
        }
}




