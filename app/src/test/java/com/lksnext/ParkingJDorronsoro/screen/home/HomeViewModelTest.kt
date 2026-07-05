package com.lksnext.ParkingJDorronsoro.screen.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.firebase.Timestamp
import com.lksnext.ParkingJDorronsoro.AparkauRoutes
import com.lksnext.ParkingJDorronsoro.common.AppText
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarManager
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarMessage
import com.lksnext.ParkingJDorronsoro.model.Plaza
import com.lksnext.ParkingJDorronsoro.model.Reserva
import com.lksnext.ParkingJDorronsoro.model.service.AccountService
import com.lksnext.ParkingJDorronsoro.model.service.AvisoSalidaService
import com.lksnext.ParkingJDorronsoro.model.service.LogService
import com.lksnext.ParkingJDorronsoro.model.service.NotificacionService
import com.lksnext.ParkingJDorronsoro.model.service.PlazaService
import com.lksnext.ParkingJDorronsoro.model.service.ReservaService
import com.lksnext.ParkingJDorronsoro.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
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
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    // ─── Rules ────────────────────────────────────────────────────────────────

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ─── Mocks ────────────────────────────────────────────────────────────────

    private val mockAccountService = mockk<AccountService>(relaxed = true)
    private val mockReservaService = mockk<ReservaService>(relaxed = true)
    private val mockPlazaService = mockk<PlazaService>(relaxed = true)
    private val mockAvisoSalidaService = mockk<AvisoSalidaService>(relaxed = true)
    private val mockNotificacionService = mockk<NotificacionService>(relaxed = true)
    private val mockLogService = mockk<LogService>(relaxed = true)

    private lateinit var viewModel: HomeViewModel

    // ─── Setup / Teardown ─────────────────────────────────────────────────────

    @Before
    fun setUp() {
        // Stubs mínimos para que cargarReservas() (llamado en init) no falle
        every { mockAccountService.currentUserId } returns "uid-test"
        coEvery { mockReservaService.getReservasActivas(any()) } returns emptyList()
        coEvery { mockReservaService.getTodasLasReservasActivas() } returns emptyList()
        coEvery { mockPlazaService.getTodasLasPlazas() } returns emptyList()

        viewModel = crearViewModel()
    }

    @After
    fun tearDown() {
        unmockkAll()
        SnackbarManager.clearSnackbarState()
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun crearViewModel() = HomeViewModel(
        mockAccountService, mockReservaService, mockPlazaService,
        mockAvisoSalidaService, mockNotificacionService, mockLogService
    )

    /**
     * Timestamp cuya fecha es HOY (Date() actual). Se usa para reservas vigentes
     * y para marcar reservas como "de hoy" en los cálculos de tandem/bloqueo.
     */
    private fun timestampHoy(): Timestamp = mockk<Timestamp>(relaxed = true).also {
        every { it.toDate() } returns Date()
    }

    /**
     * Timestamp de hace 2 días → siempre anterior a "inicio de hoy" (medianoche),
     * por lo que las reservas con este horaFin se consideran caducadas.
     */
    private fun timestampPasado(): Timestamp = mockk<Timestamp>(relaxed = true).also {
        every { it.toDate() } returns Date(System.currentTimeMillis() - 2 * 86_400_000L)
    }

    /** Reserva básica para el usuario actual programada para HOY. */
    private fun reservaHoy(
        id: String,
        plazaId: String,
        usuarioId: String = "uid-test",
        avisoSalidaEn: Timestamp? = null
    ): Reserva {
        val hoy = timestampHoy()
        return Reserva(
            id = id,
            usuarioId = usuarioId,
            plazaId = plazaId,
            horaInicio = hoy,
            horaFin = hoy,
            fechaReserva = hoy,
            avisoSalidaEn = avisoSalidaEn
        )
    }

    /** Reserva con horaFin de hace 2 días → se considerará caducada. */
    private fun reservaCaducada(id: String, plazaId: String = "1A"): Reserva {
        val pasado = timestampPasado()
        return Reserva(
            id = id,
            usuarioId = "uid-test",
            plazaId = plazaId,
            horaFin = pasado,
            fechaReserva = pasado
        )
    }

    // ─── Estado inicial (init) ────────────────────────────────────────────────

    @Test
    fun `init establece el userId en el estado`() = runTest {
        advanceUntilIdle()
        assertEquals("uid-test", viewModel.uiState.value.userId)
    }

    @Test
    fun `init isLoading es false cuando cargarReservas termina`() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `init con lista vacia deja reservas vacias en el estado`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.reservas.isEmpty())
    }

    // ─── cargarReservas ────────────────────────────────────────────────────────

    @Test
    fun `cargarReservas elimina las reservas caducadas de Firestore`() = runTest {
        coEvery { mockReservaService.getReservasActivas("uid-test") } returns
                listOf(reservaCaducada("r-vieja"))

        crearViewModel()
        advanceUntilIdle()

        coVerify { mockReservaService.eliminarReserva("r-vieja") }
    }

    @Test
    fun `cargarReservas no muestra en pantalla las reservas caducadas`() = runTest {
        coEvery { mockReservaService.getReservasActivas("uid-test") } returns
                listOf(reservaCaducada("r-vieja"))

        val vm = crearViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.reservas.isEmpty())
    }

    @Test
    fun `cargarReservas mantiene las reservas vigentes en el estado`() = runTest {
        coEvery { mockReservaService.getReservasActivas("uid-test") } returns
                listOf(reservaHoy("r-hoy", "1A"))

        val vm = crearViewModel()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.reservas.size)
        assertEquals("r-hoy", vm.uiState.value.reservas.first().reserva.id)
    }

    @Test
    fun `cargarReservas marca esHoy correctamente para reserva de hoy`() = runTest {
        coEvery { mockReservaService.getReservasActivas("uid-test") } returns
                listOf(reservaHoy("r-hoy", "1A"))

        val vm = crearViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.reservas.first().esHoy)
    }

    @Test
    fun `cargarReservas marca esTandem true si la plaza tiene plazaBloqueadaId`() = runTest {
        // Plaza "1A" es la bloqueadora: bloquea a "1B"
        coEvery { mockReservaService.getReservasActivas("uid-test") } returns
                listOf(reservaHoy("r1", "1B"))
        coEvery { mockPlazaService.getTodasLasPlazas() } returns
                listOf(Plaza(id = "1A", plazaBloqueadaId = "1B"))

        val vm = crearViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.reservas.first().esTandem)
    }

    @Test
    fun `cargarReservas marca bloqueado true cuando otro usuario ocupa la plaza bloqueadora hoy`() =
        runTest {
            // Mi reserva está en plaza "1B" (la bloqueada)
            val miReserva = reservaHoy("r-mia", "1B")
            // Otro usuario tiene hoy la plaza "1A" (la bloqueadora)
            val reservaOtro = reservaHoy("r-otro", "1A", usuarioId = "otro-uid")
            // La plaza 1A tiene plazaBloqueadaId = "1B"
            val plazaBloqueadora = Plaza(id = "1A", plazaBloqueadaId = "1B")

            coEvery { mockReservaService.getReservasActivas("uid-test") } returns listOf(miReserva)
            coEvery { mockReservaService.getTodasLasReservasActivas() } returns
                    listOf(miReserva, reservaOtro)
            coEvery { mockPlazaService.getTodasLasPlazas() } returns listOf(plazaBloqueadora)

            val vm = crearViewModel()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.reservas.first().bloqueado)
        }

    @Test
    fun `cargarReservas marca bloqueado false si la plaza bloqueadora esta libre hoy`() = runTest {
        val miReserva = reservaHoy("r-mia", "1B")
        val plazaBloqueadora = Plaza(id = "1A", plazaBloqueadaId = "1B")

        coEvery { mockReservaService.getReservasActivas("uid-test") } returns listOf(miReserva)
        // Nadie ocupa la plaza 1A hoy
        coEvery { mockReservaService.getTodasLasReservasActivas() } returns listOf(miReserva)
        coEvery { mockPlazaService.getTodasLasPlazas() } returns listOf(plazaBloqueadora)

        val vm = crearViewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.reservas.first().bloqueado)
    }

    // ─── Navegacion ────────────────────────────────────────────────────────────

    @Test
    fun `onReserveClick navega a RESERVA_SCREEN`() = runTest {
        advanceUntilIdle()
        var destino = ""
        viewModel.onReserveClick { destino = it }
        assertEquals(AparkauRoutes.RESERVA_SCREEN, destino)
    }

    @Test
    fun `onEditarReservaClick navega a EDITAR_RESERVA_SCREEN con el id`() = runTest {
        advanceUntilIdle()
        var destino = ""
        viewModel.onEditarReservaClick("r-abc") { destino = it }
        assertEquals("${AparkauRoutes.EDITAR_RESERVA_SCREEN}/r-abc", destino)
    }

    @Test
    fun `onMiCuentaClick navega a MI_CUENTA_SCREEN`() = runTest {
        advanceUntilIdle()
        var destino = ""
        viewModel.onMiCuentaClick { destino = it }
        assertEquals(AparkauRoutes.MI_CUENTA_SCREEN, destino)
    }

    // ─── onEliminarReservaClick ────────────────────────────────────────────────

    @Test
    fun `onEliminarReservaClick cancela los recordatorios locales`() = runTest {
        coEvery { mockReservaService.getReservasActivas("uid-test") } returns
                listOf(reservaHoy("r1", "1A"))
        val vm = crearViewModel()
        advanceUntilIdle()

        vm.onEliminarReservaClick("r1")
        advanceUntilIdle()

        coVerify { mockNotificacionService.cancelarRecordatorios("r1") }
    }

    @Test
    fun `onEliminarReservaClick llama a eliminarReserva en el servicio`() = runTest {
        coEvery { mockReservaService.getReservasActivas("uid-test") } returns
                listOf(reservaHoy("r1", "1A"))
        val vm = crearViewModel()
        advanceUntilIdle()

        vm.onEliminarReservaClick("r1")
        advanceUntilIdle()

        coVerify { mockReservaService.eliminarReserva("r1") }
    }

    @Test
    fun `onEliminarReservaClick elimina la reserva del estado local`() = runTest {
        coEvery { mockReservaService.getReservasActivas("uid-test") } returns
                listOf(reservaHoy("r1", "1A"))
        val vm = crearViewModel()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.reservas.size)

        vm.onEliminarReservaClick("r1")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.reservas.isEmpty())
    }

    @Test
    fun `onEliminarReservaClick muestra snackbar de reserva eliminada`() = runTest {
        advanceUntilIdle()

        viewModel.onEliminarReservaClick("r1")
        advanceUntilIdle()

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.reserva_eliminada, (mensaje as SnackbarMessage.ResourceSnackbar).message)
    }

    // ─── onAvisarSalidaClick ───────────────────────────────────────────────────

    @Test
    fun `onAvisarSalidaClick con plazaId desconocido no llama al servicio`() = runTest {
        advanceUntilIdle()

        viewModel.onAvisarSalidaClick("plazaQueNoExiste")
        advanceUntilIdle()

        coVerify(exactly = 0) { mockAvisoSalidaService.solicitarSalida(any(), any()) }
    }

    @Test
    fun `onAvisarSalidaClick con reserva no bloqueada no llama al servicio`() = runTest {
        // Plaza sin tandem → bloqueado = false
        coEvery { mockReservaService.getReservasActivas("uid-test") } returns
                listOf(reservaHoy("r1", "1A"))
        coEvery { mockPlazaService.getTodasLasPlazas() } returns
                listOf(Plaza(id = "1A", plazaBloqueadaId = ""))

        val vm = crearViewModel()
        advanceUntilIdle()

        vm.onAvisarSalidaClick("1A")
        advanceUntilIdle()

        coVerify(exactly = 0) { mockAvisoSalidaService.solicitarSalida(any(), any()) }
    }

    @Test
    fun `onAvisarSalidaClick con reserva bloqueada llama al servicio y muestra snackbar`() =
        runTest {
            val miReserva = reservaHoy("r-mia", "1B")
            val reservaOtro = reservaHoy("r-otro", "1A", usuarioId = "otro-uid")
            val plazaBloqueadora = Plaza(id = "1A", plazaBloqueadaId = "1B")

            coEvery { mockReservaService.getReservasActivas("uid-test") } returns listOf(miReserva)
            coEvery { mockReservaService.getTodasLasReservasActivas() } returns
                    listOf(miReserva, reservaOtro)
            coEvery { mockPlazaService.getTodasLasPlazas() } returns listOf(plazaBloqueadora)

            val vm = crearViewModel()
            advanceUntilIdle()

            vm.onAvisarSalidaClick("1B")
            advanceUntilIdle()

            coVerify { mockAvisoSalidaService.solicitarSalida("uid-test", "1B") }
            val mensaje = SnackbarManager.snackbarMessages.value
            assertNotNull(mensaje)
            assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
            assertEquals(
                AppText.aviso_salida_enviado,
                (mensaje as SnackbarMessage.ResourceSnackbar).message
            )
        }

    @Test
    fun `onAvisarSalidaClick marca avisoEnviado true de inmediato en el estado`() = runTest {
        val miReserva = reservaHoy("r-mia", "1B")
        val reservaOtro = reservaHoy("r-otro", "1A", usuarioId = "otro-uid")
        val plazaBloqueadora = Plaza(id = "1A", plazaBloqueadaId = "1B")

        coEvery { mockReservaService.getReservasActivas("uid-test") } returns listOf(miReserva)
        coEvery { mockReservaService.getTodasLasReservasActivas() } returns
                listOf(miReserva, reservaOtro)
        coEvery { mockPlazaService.getTodasLasPlazas() } returns listOf(plazaBloqueadora)

        val vm = crearViewModel()
        advanceUntilIdle()

        vm.onAvisarSalidaClick("1B")
        // No hace falta advanceUntilIdle(): avisoEnviado se marca ANTES del launchCatching

        assertTrue(vm.uiState.value.reservas.first { it.reserva.plazaId == "1B" }.avisoEnviado)
    }

    @Test
    fun `onAvisarSalidaClick enviado dos veces solo llama al servicio una vez`() = runTest {
        val miReserva = reservaHoy("r-mia", "1B")
        val reservaOtro = reservaHoy("r-otro", "1A", usuarioId = "otro-uid")
        val plazaBloqueadora = Plaza(id = "1A", plazaBloqueadaId = "1B")

        coEvery { mockReservaService.getReservasActivas("uid-test") } returns listOf(miReserva)
        coEvery { mockReservaService.getTodasLasReservasActivas() } returns
                listOf(miReserva, reservaOtro)
        coEvery { mockPlazaService.getTodasLasPlazas() } returns listOf(plazaBloqueadora)

        val vm = crearViewModel()
        advanceUntilIdle()

        vm.onAvisarSalidaClick("1B") // primer aviso → OK
        advanceUntilIdle()
        vm.onAvisarSalidaClick("1B") // segundo aviso → la guardia lo bloquea
        advanceUntilIdle()

        // El servicio solo debe haberse llamado UNA VEZ
        coVerify(exactly = 1) { mockAvisoSalidaService.solicitarSalida(any(), any()) }
    }

    // ─── onAvisoSalidaVistoClick ───────────────────────────────────────────────

    @Test
    fun `onAvisoSalidaVistoClick llama a marcarAvisoSalidaVisto`() = runTest {
        advanceUntilIdle()

        viewModel.onAvisoSalidaVistoClick("r1")
        advanceUntilIdle()

        coVerify { mockReservaService.marcarAvisoSalidaVisto("r1") }
    }

    @Test
    fun `onAvisoSalidaVistoClick pone avisoSalidaPendiente a false en la reserva`() = runTest {
        val hoy = timestampHoy()
        // avisoSalidaEn = hoy → el ViewModel calcula avisoSalidaPendiente = true
        val miReserva = reservaHoy("r1", "1A", avisoSalidaEn = hoy)

        coEvery { mockReservaService.getReservasActivas("uid-test") } returns listOf(miReserva)

        val vm = crearViewModel()
        advanceUntilIdle()

        // Antes del click: avisoSalidaPendiente debe ser true
        assertTrue(vm.uiState.value.reservas.first().avisoSalidaPendiente)

        vm.onAvisoSalidaVistoClick("r1")
        // El estado se actualiza de forma SÍNCRONA antes del launchCatching
        assertFalse(vm.uiState.value.reservas.first().avisoSalidaPendiente)
    }
}




