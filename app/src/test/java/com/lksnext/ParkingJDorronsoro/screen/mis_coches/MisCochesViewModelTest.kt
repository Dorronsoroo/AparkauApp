package com.lksnext.ParkingJDorronsoro.screen.mis_coches

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.lksnext.ParkingJDorronsoro.AparkauRoutes
import com.lksnext.ParkingJDorronsoro.common.AppText
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarManager
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarMessage
import com.lksnext.ParkingJDorronsoro.model.Vehiculo
import com.lksnext.ParkingJDorronsoro.model.service.AccountService
import com.lksnext.ParkingJDorronsoro.model.service.LogService
import com.lksnext.ParkingJDorronsoro.model.service.VehiculoService
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

@OptIn(ExperimentalCoroutinesApi::class)
class MisCochesViewModelTest {

    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val mockAccountService = mockk<AccountService>(relaxed = true)
    private val mockVehiculoService = mockk<VehiculoService>(relaxed = true)
    private val mockLogService = mockk<LogService>(relaxed = true)

    private lateinit var viewModel: MisCochesViewModel

    @Before
    fun setUp() {
        every { mockAccountService.currentUserId } returns "uid-test"
        coEvery { mockVehiculoService.getVehiculos(any()) } returns emptyList()
        viewModel = MisCochesViewModel(mockAccountService, mockVehiculoService, mockLogService)
    }

    @After
    fun tearDown() {
        unmockkAll()
        SnackbarManager.clearSnackbarState()
    }

    // ─── init: cargarVehiculos ────────────────────────────────────────────────

    @Test
    fun `init carga la lista de vehiculos en el estado`() = runTest {
        val vehiculos = listOf(Vehiculo("1234-ABC", "Seat"), Vehiculo("5678-XYZ", "Ford"))
        coEvery { mockVehiculoService.getVehiculos("uid-test") } returns vehiculos

        val vm = MisCochesViewModel(mockAccountService, mockVehiculoService, mockLogService)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.vehiculos.size)
    }

    @Test
    fun `init isLoading es false cuando termina la carga`() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── Actualización de estado ───────────────────────────────────────────────

    @Test
    fun `onMatriculaChange convierte la matricula a mayusculas`() = runTest {
        advanceUntilIdle()
        viewModel.onMatriculaChange("1234-abc")
        assertEquals("1234-ABC", viewModel.uiState.value.matricula)
    }

    @Test
    fun `onModeloChange actualiza el modelo en el estado`() = runTest {
        advanceUntilIdle()
        viewModel.onModeloChange("Toyota Yaris")
        assertEquals("Toyota Yaris", viewModel.uiState.value.modelo)
    }

    // ─── onAddVehiculoClick: validaciones ─────────────────────────────────────

    @Test
    fun `onAddVehiculoClick con matricula vacia muestra error de matricula`() = runTest {
        advanceUntilIdle()
        viewModel.onMatriculaChange("")
        viewModel.onModeloChange("Seat Ibiza")

        viewModel.onAddVehiculoClick()

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.matricula_error, (mensaje as SnackbarMessage.ResourceSnackbar).message)
        coVerify(exactly = 0) { mockVehiculoService.agregarVehiculo(any(), any()) }
    }

    @Test
    fun `onAddVehiculoClick con modelo vacio muestra error de modelo`() = runTest {
        advanceUntilIdle()
        viewModel.onMatriculaChange("1234-ABC")
        viewModel.onModeloChange("")

        viewModel.onAddVehiculoClick()

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.modelo_error, (mensaje as SnackbarMessage.ResourceSnackbar).message)
        coVerify(exactly = 0) { mockVehiculoService.agregarVehiculo(any(), any()) }
    }

    // ─── onAddVehiculoClick: flujo exitoso ────────────────────────────────────

    @Test
    fun `onAddVehiculoClick valido llama a agregarVehiculo con trim aplicado`() = runTest {
        coEvery { mockVehiculoService.agregarVehiculo(any(), any()) } returns Result.success(Unit)

        viewModel.onMatriculaChange("  1234-ABC  ") // spaces se eliminan con trim()
        viewModel.onModeloChange("  Seat Ibiza  ")
        viewModel.onAddVehiculoClick()
        advanceUntilIdle()

        coVerify {
            mockVehiculoService.agregarVehiculo(
                uid = "uid-test",
                vehiculo = match { it.matricula == "1234-ABC" && it.modelo == "Seat Ibiza" }
            )
        }
    }

    @Test
    fun `onAddVehiculoClick valido limpia los campos del formulario`() = runTest {
        coEvery { mockVehiculoService.agregarVehiculo(any(), any()) } returns Result.success(Unit)

        viewModel.onMatriculaChange("1234-ABC")
        viewModel.onModeloChange("Seat Ibiza")
        viewModel.onAddVehiculoClick()
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.matricula)
        assertEquals("", viewModel.uiState.value.modelo)
    }

    @Test
    fun `onAddVehiculoClick valido muestra snackbar de vehiculo guardado`() = runTest {
        coEvery { mockVehiculoService.agregarVehiculo(any(), any()) } returns Result.success(Unit)

        viewModel.onMatriculaChange("1234-ABC")
        viewModel.onModeloChange("Seat Ibiza")
        viewModel.onAddVehiculoClick()
        advanceUntilIdle()

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.vehiculo_guardado, (mensaje as SnackbarMessage.ResourceSnackbar).message)
    }

    // ─── onEliminarVehiculoClick ───────────────────────────────────────────────

    @Test
    fun `onEliminarVehiculoClick llama al servicio con el vehiculo correcto`() = runTest {
        val vehiculo = Vehiculo("1234-ABC", "Seat")
        coEvery { mockVehiculoService.eliminarVehiculo(any(), any()) } returns Result.success(Unit)

        viewModel.onEliminarVehiculoClick(vehiculo)
        advanceUntilIdle()

        coVerify { mockVehiculoService.eliminarVehiculo("uid-test", vehiculo) }
    }

    @Test
    fun `onEliminarVehiculoClick recarga la lista tras eliminar`() = runTest {
        val vehiculo = Vehiculo("1234-ABC", "Seat")
        coEvery { mockVehiculoService.eliminarVehiculo(any(), any()) } returns Result.success(Unit)

        viewModel.onEliminarVehiculoClick(vehiculo)
        advanceUntilIdle()

        // getVehiculos se llama en init (1) + tras eliminar (1) = 2 veces
        coVerify(exactly = 2) { mockVehiculoService.getVehiculos("uid-test") }
    }

    // ─── onVolverClick ────────────────────────────────────────────────────────

    @Test
    fun `onVolverClick navega a MI_CUENTA y hace pop de MIS_COCHES`() = runTest {
        advanceUntilIdle()
        var destino = ""; var popUp = ""

        viewModel.onVolverClick { dest, pop -> destino = dest; popUp = pop }

        assertEquals(AparkauRoutes.MI_CUENTA_SCREEN, destino)
        assertEquals(AparkauRoutes.MIS_COCHES_SCREEN, popUp)
    }
}
