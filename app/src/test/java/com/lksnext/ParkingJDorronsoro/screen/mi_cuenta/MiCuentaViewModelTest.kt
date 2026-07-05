package com.lksnext.ParkingJDorronsoro.screen.mi_cuenta

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.lksnext.ParkingJDorronsoro.AparkauRoutes
import com.lksnext.ParkingJDorronsoro.common.AppText
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarManager
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarMessage
import com.lksnext.ParkingJDorronsoro.model.PerfilUsuario
import com.lksnext.ParkingJDorronsoro.model.Usuario
import com.lksnext.ParkingJDorronsoro.model.service.AccountService
import com.lksnext.ParkingJDorronsoro.model.service.LogService
import com.lksnext.ParkingJDorronsoro.model.service.NotificacionService
import com.lksnext.ParkingJDorronsoro.model.service.UsuarioService
import com.lksnext.ParkingJDorronsoro.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
class MiCuentaViewModelTest {

    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val mockAccountService = mockk<AccountService>(relaxed = true)
    private val mockUsuarioService = mockk<UsuarioService>(relaxed = true)
    private val mockNotificacionService = mockk<NotificacionService>(relaxed = true)
    private val mockLogService = mockk<LogService>(relaxed = true)

    private lateinit var viewModel: MiCuentaViewModel

    @Before
    fun setUp() {
        every { mockAccountService.currentUserId } returns "uid-test"
        coEvery { mockUsuarioService.getUsuario(any()) } returns null
        viewModel = MiCuentaViewModel(
            mockAccountService, mockUsuarioService, mockNotificacionService, mockLogService
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
        SnackbarManager.clearSnackbarState()
    }

    // ─── init: cargarUsuario ──────────────────────────────────────────────────

    @Test
    fun `init pre-rellena el email y perfil con los datos del usuario`() = runTest {
        coEvery { mockUsuarioService.getUsuario("uid-test") } returns
                Usuario(email = "jon@test.com", perfil = PerfilUsuario.VIP_CLIENTE)

        val vm = MiCuentaViewModel(
            mockAccountService, mockUsuarioService, mockNotificacionService, mockLogService
        )
        advanceUntilIdle()

        assertEquals("jon@test.com", vm.uiState.value.email)
        assertEquals(PerfilUsuario.VIP_CLIENTE, vm.uiState.value.perfil)
    }

    @Test
    fun `init con usuario null deja el estado con valores por defecto`() = runTest {
        advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.email)
        assertEquals(PerfilUsuario.EMPLEADO_HABITUAL, viewModel.uiState.value.perfil)
    }

    @Test
    fun `init isLoading es false cuando termina la carga`() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── Actualización de estado ───────────────────────────────────────────────

    @Test
    fun `onEmailChange actualiza el email en el estado`() = runTest {
        advanceUntilIdle()
        viewModel.onEmailChange("nuevo@email.com")
        assertEquals("nuevo@email.com", viewModel.uiState.value.email)
    }

    // ─── onGuardarClick ───────────────────────────────────────────────────────

    @Test
    fun `onGuardarClick con email vacio muestra error de email`() = runTest {
        advanceUntilIdle()
        viewModel.onEmailChange("")

        viewModel.onGuardarClick()

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.email_error, (mensaje as SnackbarMessage.ResourceSnackbar).message)
        coVerify(exactly = 0) { mockUsuarioService.actualizarUsuario(any(), any(), any()) }
    }

    @Test
    fun `onGuardarClick valido llama a actualizarUsuario con email trimmeado`() = runTest {
        coEvery { mockUsuarioService.actualizarUsuario(any(), any(), any()) } returns Result.success(Unit)
        advanceUntilIdle()

        viewModel.onEmailChange("  jon@test.com  ")
        viewModel.onGuardarClick()
        advanceUntilIdle()

        coVerify {
            mockUsuarioService.actualizarUsuario(
                uid = "uid-test",
                email = "jon@test.com", // trim() aplicado
                perfil = PerfilUsuario.EMPLEADO_HABITUAL
            )
        }
    }

    @Test
    fun `onGuardarClick valido muestra snackbar de datos guardados`() = runTest {
        coEvery { mockUsuarioService.actualizarUsuario(any(), any(), any()) } returns Result.success(Unit)
        advanceUntilIdle()

        viewModel.onEmailChange("jon@test.com")
        viewModel.onGuardarClick()
        advanceUntilIdle()

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.datos_guardados, (mensaje as SnackbarMessage.ResourceSnackbar).message)
    }

    // ─── Navegación ───────────────────────────────────────────────────────────

    @Test
    fun `onMisCochesClick navega a MIS_COCHES_SCREEN`() = runTest {
        advanceUntilIdle()
        var destino = ""
        viewModel.onMisCochesClick { destino = it }
        assertEquals(AparkauRoutes.MIS_COCHES_SCREEN, destino)
    }

    @Test
    fun `onVolverClick navega a HOME y hace pop de MI_CUENTA`() = runTest {
        advanceUntilIdle()
        var destino = ""; var popUp = ""
        viewModel.onVolverClick { dest, pop -> destino = dest; popUp = pop }
        assertEquals(AparkauRoutes.HOME_SCREEN, destino)
        assertEquals(AparkauRoutes.MI_CUENTA_SCREEN, popUp)
    }

    // ─── onSignOutClick ───────────────────────────────────────────────────────

    @Test
    fun `onSignOutClick elimina el token FCM ANTES de cerrar sesion`() = runTest {
        coEvery { mockNotificacionService.eliminarTokenActual() } just runs
        coEvery { mockAccountService.signOut() } just runs
        advanceUntilIdle()

        viewModel.onSignOutClick { _, _ -> }
        advanceUntilIdle()

        // El orden importa: token eliminado primero, luego signOut
        coVerifyOrder {
            mockNotificacionService.eliminarTokenActual()
            mockAccountService.signOut()
        }
    }

    @Test
    fun `onSignOutClick navega a LOGIN y hace pop de HOME`() = runTest {
        coEvery { mockNotificacionService.eliminarTokenActual() } just runs
        coEvery { mockAccountService.signOut() } just runs
        advanceUntilIdle()

        var destino = ""; var popUp = ""
        viewModel.onSignOutClick { dest, pop -> destino = dest; popUp = pop }
        advanceUntilIdle()

        assertEquals(AparkauRoutes.LOGIN_SCREEN, destino)
        assertEquals(AparkauRoutes.HOME_SCREEN, popUp)
    }
}
