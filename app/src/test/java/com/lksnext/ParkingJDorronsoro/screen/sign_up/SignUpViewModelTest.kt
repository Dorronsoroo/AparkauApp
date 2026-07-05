package com.lksnext.ParkingJDorronsoro.screen.sign_up

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.lksnext.ParkingJDorronsoro.AparkauRoutes
import com.lksnext.ParkingJDorronsoro.common.AppText
import com.lksnext.ParkingJDorronsoro.common.ext.isValidEmail
import com.lksnext.ParkingJDorronsoro.common.ext.isValidPassword
import com.lksnext.ParkingJDorronsoro.common.ext.passwordMatches
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarManager
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarMessage
import com.lksnext.ParkingJDorronsoro.model.PerfilUsuario
import com.lksnext.ParkingJDorronsoro.model.service.AccountService
import com.lksnext.ParkingJDorronsoro.model.service.LogService
import com.lksnext.ParkingJDorronsoro.model.service.NotificacionService
import com.lksnext.ParkingJDorronsoro.model.service.UsuarioService
import com.lksnext.ParkingJDorronsoro.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {

    // ─── Rules ────────────────────────────────────────────────────────────────

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ─── Mocks ────────────────────────────────────────────────────────────────

    private val mockAccountService = mockk<AccountService>(relaxed = true)
    private val mockUsuarioService = mockk<UsuarioService>(relaxed = true)
    private val mockNotificacionService = mockk<NotificacionService>(relaxed = true)
    private val mockLogService = mockk<LogService>(relaxed = true)

    private lateinit var viewModel: SignUpViewModel

    // ─── Setup / Teardown ─────────────────────────────────────────────────────

    @Before
    fun setUp() {
        // isValidEmail, isValidPassword y passwordMatches están en el mismo fichero,
        // así que un solo mockkStatic cubre las tres funciones de extensión.
        mockkStatic("com.lksnext.ParkingJDorronsoro.common.ext.StringExtensionsKt")
        viewModel = SignUpViewModel(
            mockAccountService,
            mockUsuarioService,
            mockNotificacionService,
            mockLogService
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
        SnackbarManager.clearSnackbarState()
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Configura todas las validaciones como válidas de golpe. */
    private fun todasLasValidacionesValidas() {
        every { any<String>().isValidEmail() } returns true
        every { any<String>().isValidPassword() } returns true
        every { any<String>().passwordMatches(any()) } returns true
    }

    /** Rellena el formulario con datos de ejemplo válidos. */
    private fun rellenarFormularioValido() {
        viewModel.onNombreChange("Jon")
        viewModel.onApellidosChange("Dorronsoro")
        viewModel.onEmailChange("jon@example.com")
        viewModel.onPasswordChange("Password1!")
        viewModel.onRepeatPasswordChange("Password1!")
        viewModel.onPerfilChange(PerfilUsuario.EMPLEADO_HABITUAL)
    }

    // ─── Estado inicial ────────────────────────────────────────────────────────

    @Test
    fun `estado inicial tiene todos los campos vacios y perfil por defecto`() {
        assertEquals("", viewModel.uiState.value.nombre)
        assertEquals("", viewModel.uiState.value.apellidos)
        assertEquals("", viewModel.uiState.value.email)
        assertEquals("", viewModel.uiState.value.password)
        assertEquals("", viewModel.uiState.value.repeatPassword)
        assertEquals(PerfilUsuario.EMPLEADO_HABITUAL, viewModel.uiState.value.perfil)
    }

    // ─── Actualizacion de estado ───────────────────────────────────────────────

    @Test
    fun `onNombreChange actualiza el nombre en el uiState`() {
        viewModel.onNombreChange("Jon")
        assertEquals("Jon", viewModel.uiState.value.nombre)
    }

    @Test
    fun `onApellidosChange actualiza los apellidos en el uiState`() {
        viewModel.onApellidosChange("Dorronsoro")
        assertEquals("Dorronsoro", viewModel.uiState.value.apellidos)
    }

    @Test
    fun `onEmailChange actualiza el email en el uiState`() {
        viewModel.onEmailChange("test@test.com")
        assertEquals("test@test.com", viewModel.uiState.value.email)
    }

    @Test
    fun `onPasswordChange actualiza la contrasena en el uiState`() {
        viewModel.onPasswordChange("Password1!")
        assertEquals("Password1!", viewModel.uiState.value.password)
    }

    @Test
    fun `onRepeatPasswordChange actualiza la repeticion en el uiState`() {
        viewModel.onRepeatPasswordChange("Password1!")
        assertEquals("Password1!", viewModel.uiState.value.repeatPassword)
    }

    @Test
    fun `onPerfilChange actualiza el perfil en el uiState`() {
        viewModel.onPerfilChange(PerfilUsuario.VIP_CLIENTE)
        assertEquals(PerfilUsuario.VIP_CLIENTE, viewModel.uiState.value.perfil)
    }

    // ─── onSignUpClick: validaciones (orden del ViewModel) ────────────────────

    @Test
    fun `onSignUpClick con nombre vacio muestra error de nombre`() {
        viewModel.onNombreChange("") // nombre en blanco

        viewModel.onSignUpClick { _, _ -> }

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.nombre_error, (mensaje as SnackbarMessage.ResourceSnackbar).message)
        coVerify(exactly = 0) { mockAccountService.createAccount(any(), any()) }
    }

    @Test
    fun `onSignUpClick con apellidos vacios muestra error de apellidos`() {
        viewModel.onNombreChange("Jon")
        viewModel.onApellidosChange("") // apellidos en blanco

        viewModel.onSignUpClick { _, _ -> }

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.apellidos_error, (mensaje as SnackbarMessage.ResourceSnackbar).message)
        coVerify(exactly = 0) { mockAccountService.createAccount(any(), any()) }
    }

    @Test
    fun `onSignUpClick con email invalido muestra error de email`() {
        every { any<String>().isValidEmail() } returns false
        viewModel.onNombreChange("Jon")
        viewModel.onApellidosChange("Dorronsoro")
        viewModel.onEmailChange("noesunemail")

        viewModel.onSignUpClick { _, _ -> }

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.email_error, (mensaje as SnackbarMessage.ResourceSnackbar).message)
        coVerify(exactly = 0) { mockAccountService.createAccount(any(), any()) }
    }

    @Test
    fun `onSignUpClick con contrasena invalida muestra error de contrasena`() {
        every { any<String>().isValidEmail() } returns true
        every { any<String>().isValidPassword() } returns false
        viewModel.onNombreChange("Jon")
        viewModel.onApellidosChange("Dorronsoro")
        viewModel.onEmailChange("jon@example.com")
        viewModel.onPasswordChange("debil")

        viewModel.onSignUpClick { _, _ -> }

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.password_error, (mensaje as SnackbarMessage.ResourceSnackbar).message)
        coVerify(exactly = 0) { mockAccountService.createAccount(any(), any()) }
    }

    @Test
    fun `onSignUpClick con contrasenas que no coinciden muestra error de coincidencia`() {
        every { any<String>().isValidEmail() } returns true
        every { any<String>().isValidPassword() } returns true
        every { any<String>().passwordMatches(any()) } returns false
        viewModel.onNombreChange("Jon")
        viewModel.onApellidosChange("Dorronsoro")
        viewModel.onEmailChange("jon@example.com")
        viewModel.onPasswordChange("Password1!")
        viewModel.onRepeatPasswordChange("Password2!")

        viewModel.onSignUpClick { _, _ -> }

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(
            AppText.password_match_error,
            (mensaje as SnackbarMessage.ResourceSnackbar).message
        )
        coVerify(exactly = 0) { mockAccountService.createAccount(any(), any()) }
    }

    // ─── onSignUpClick: flujo exitoso ──────────────────────────────────────────

    @Test
    fun `onSignUpClick valido llama a createAccount con email y contrasena correctos`() =
        runTest {
            todasLasValidacionesValidas()
            coEvery { mockAccountService.createAccount(any(), any()) } just runs
            coEvery { mockUsuarioService.guardarUsuario(any(), any(), any(), any(), any()) } returns
                    Result.success(Unit)
            coEvery { mockNotificacionService.registrarTokenActual() } just runs
            every { mockAccountService.currentUserId } returns "uid-test-123"

            rellenarFormularioValido()
            viewModel.onSignUpClick { _, _ -> }
            advanceUntilIdle()

            coVerify { mockAccountService.createAccount("jon@example.com", "Password1!") }
        }

    @Test
    fun `onSignUpClick valido llama a guardarUsuario con nombre trimmeado`() = runTest {
        todasLasValidacionesValidas()
        coEvery { mockAccountService.createAccount(any(), any()) } just runs
        coEvery { mockUsuarioService.guardarUsuario(any(), any(), any(), any(), any()) } returns
                Result.success(Unit)
        coEvery { mockNotificacionService.registrarTokenActual() } just runs
        every { mockAccountService.currentUserId } returns "uid-test-123"

        // Nombre con espacios extra para verificar que se hace trim()
        viewModel.onNombreChange("  Jon  ")
        viewModel.onApellidosChange("  Dorronsoro  ")
        viewModel.onEmailChange("jon@example.com")
        viewModel.onPasswordChange("Password1!")
        viewModel.onRepeatPasswordChange("Password1!")

        viewModel.onSignUpClick { _, _ -> }
        advanceUntilIdle()

        coVerify {
            mockUsuarioService.guardarUsuario(
                uid = "uid-test-123",
                nombre = "Jon",           // trim() aplicado
                apellidos = "Dorronsoro", // trim() aplicado
                email = "jon@example.com",
                perfil = PerfilUsuario.EMPLEADO_HABITUAL
            )
        }
    }

    @Test
    fun `onSignUpClick valido navega a HOME y hace pop de SIGN_UP`() = runTest {
        todasLasValidacionesValidas()
        coEvery { mockAccountService.createAccount(any(), any()) } just runs
        coEvery { mockUsuarioService.guardarUsuario(any(), any(), any(), any(), any()) } returns
                Result.success(Unit)
        coEvery { mockNotificacionService.registrarTokenActual() } just runs
        every { mockAccountService.currentUserId } returns "uid-test-123"

        var destino = ""
        var popUp = ""
        rellenarFormularioValido()
        viewModel.onSignUpClick { dest, pop ->
            destino = dest
            popUp = pop
        }
        advanceUntilIdle()

        assertEquals(AparkauRoutes.HOME_SCREEN, destino)
        assertEquals(AparkauRoutes.SIGN_UP_SCREEN, popUp)
    }

    @Test
    fun `onSignUpClick valido registra el token FCM`() = runTest {
        todasLasValidacionesValidas()
        coEvery { mockAccountService.createAccount(any(), any()) } just runs
        coEvery { mockUsuarioService.guardarUsuario(any(), any(), any(), any(), any()) } returns
                Result.success(Unit)
        coEvery { mockNotificacionService.registrarTokenActual() } just runs
        every { mockAccountService.currentUserId } returns "uid-test-123"

        rellenarFormularioValido()
        viewModel.onSignUpClick { _, _ -> }
        advanceUntilIdle()

        coVerify { mockNotificacionService.registrarTokenActual() }
    }

    @Test
    fun `onSignUpClick cuando guardarUsuario falla no navega`() = runTest {
        todasLasValidacionesValidas()
        coEvery { mockAccountService.createAccount(any(), any()) } just runs
        every { mockAccountService.currentUserId } returns "uid-test-123"
        // Firestore devuelve un error
        coEvery { mockUsuarioService.guardarUsuario(any(), any(), any(), any(), any()) } returns
                Result.failure(Exception("Error de Firestore"))

        var navegoAHome = false
        rellenarFormularioValido()
        viewModel.onSignUpClick { dest, _ ->
            if (dest == AparkauRoutes.HOME_SCREEN) navegoAHome = true
        }
        advanceUntilIdle()

        assertTrue("No deberia haber navegado a Home", !navegoAHome)
    }

    @Test
    fun `onSignUpClick valido no deja mensaje de error en el snackbar`() = runTest {
        todasLasValidacionesValidas()
        coEvery { mockAccountService.createAccount(any(), any()) } just runs
        coEvery { mockUsuarioService.guardarUsuario(any(), any(), any(), any(), any()) } returns
                Result.success(Unit)
        coEvery { mockNotificacionService.registrarTokenActual() } just runs
        every { mockAccountService.currentUserId } returns "uid-test-123"

        rellenarFormularioValido()
        viewModel.onSignUpClick { _, _ -> }
        advanceUntilIdle()

        assertNull(SnackbarManager.snackbarMessages.value)
    }

    // ─── onLoginClick ──────────────────────────────────────────────────────────

    @Test
    fun `onLoginClick navega a LOGIN y hace pop de SIGN_UP`() {
        var destino = ""
        var popUp = ""

        viewModel.onLoginClick { dest, pop ->
            destino = dest
            popUp = pop
        }

        assertEquals(AparkauRoutes.LOGIN_SCREEN, destino)
        assertEquals(AparkauRoutes.SIGN_UP_SCREEN, popUp)
    }
}

