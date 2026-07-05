package com.lksnext.ParkingJDorronsoro.screen.login

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.lksnext.ParkingJDorronsoro.AparkauRoutes
import com.lksnext.ParkingJDorronsoro.common.AppText
import com.lksnext.ParkingJDorronsoro.common.ext.isValidEmail
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarManager
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarMessage
import com.lksnext.ParkingJDorronsoro.model.service.AccountService
import com.lksnext.ParkingJDorronsoro.model.service.LogService
import com.lksnext.ParkingJDorronsoro.model.service.NotificacionService
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

/**
 * Tests unitarios para LoginViewModel.
 *
 * Conceptos clave usados aquí:
 *
 * - mockk<T>(): crea un "doble" de una interfaz/clase que NO hace nada real.
 *   Con relaxed = true, los métodos no configurados devuelven valores por defecto
 *   sin lanzar errores.
 *
 * - coEvery { ... } returns / just runs: define QUÉ hace el mock cuando se llama
 *   a una función suspend (co = coroutine).
 *
 * - coVerify { ... }: comprueba que una función suspend se llamó (y con qué args).
 *
 * - mockkStatic(...): permite mockear funciones estáticas/de extensión de Kotlin.
 *   Necesario porque isValidEmail() usa android.util.Patterns (API de Android).
 *
 * - InstantTaskExecutorRule: hace que el executor de Architecture Components
 *   (LiveData, etc.) sea síncrono en tests.
 *
 * - MainDispatcherRule: sustituye Dispatchers.Main por un dispatcher de test,
 *   necesario para que viewModelScope.launch funcione en JVM sin emulador.
 *
 * - runTest { }: bloque para testear código con coroutines/suspend.
 * - advanceUntilIdle(): avanza el tiempo virtual hasta que todas las coroutines
 *   pendientes terminen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    // ─── Rules (se aplican antes/después de cada @Test automáticamente) ────────

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ─── Mocks de los servicios ────────────────────────────────────────────────

    // relaxed = true → los métodos no configurados no lanzan error, devuelven defaults
    private val mockAccountService = mockk<AccountService>(relaxed = true)
    private val mockNotificacionService = mockk<NotificacionService>(relaxed = true)
    private val mockLogService = mockk<LogService>(relaxed = true)

    private lateinit var viewModel: LoginViewModel

    // ─── Setup y teardown ─────────────────────────────────────────────────────

    @Before
    fun setUp() {
        // isValidEmail() usa android.util.Patterns, que no existe en la JVM.
        // Con mockkStatic interceptamos la función de extensión para controlarla.
        mockkStatic("com.lksnext.ParkingJDorronsoro.common.ext.StringExtensionsKt")

        viewModel = LoginViewModel(mockAccountService, mockNotificacionService, mockLogService)
    }

    @After
    fun tearDown() {
        unmockkAll()                        // limpia todos los mocks estáticos
        SnackbarManager.clearSnackbarState() // resetea el estado global del snackbar
    }

    // ─── Estado inicial ────────────────────────────────────────────────────────

    @Test
    fun `estado inicial tiene email y contraseña vacíos`() {
        assertEquals("", viewModel.uiState.value.email)
        assertEquals("", viewModel.uiState.value.password)
    }

    // ─── Actualización de estado ───────────────────────────────────────────────

    @Test
    fun `onEmailChange actualiza el email en el uiState`() {
        viewModel.onEmailChange("usuario@ejemplo.com")

        assertEquals("usuario@ejemplo.com", viewModel.uiState.value.email)
    }

    @Test
    fun `onPasswordChange actualiza la contraseña en el uiState`() {
        viewModel.onPasswordChange("MiContraseña1!")

        assertEquals("MiContraseña1!", viewModel.uiState.value.password)
    }

    @Test
    fun `cambios sucesivos de email conservan el último valor`() {
        viewModel.onEmailChange("primero@test.com")
        viewModel.onEmailChange("segundo@test.com")

        assertEquals("segundo@test.com", viewModel.uiState.value.email)
    }

    // ─── onSignInClick: validaciones ─────────────────────────────────────────

    @Test
    fun `onSignInClick con email inválido muestra error y no llama al servicio`() {
        every { any<String>().isValidEmail() } returns false // email inválido
        viewModel.onEmailChange("noesunemail")

        viewModel.onSignInClick { _, _ -> }

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.email_error, (mensaje as SnackbarMessage.ResourceSnackbar).message)
        // El servicio NO debe haberse llamado
        coVerify(exactly = 0) { mockAccountService.authenticate(any(), any()) }
    }

    @Test
    fun `onSignInClick con contraseña en blanco muestra error de contraseña`() {
        every { any<String>().isValidEmail() } returns true // email "correcto"
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("   ") // solo espacios = isBlank() → true

        viewModel.onSignInClick { _, _ -> }

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(
            AppText.empty_password_error,
            (mensaje as SnackbarMessage.ResourceSnackbar).message
        )
        coVerify(exactly = 0) { mockAccountService.authenticate(any(), any()) }
    }

    @Test
    fun `onSignInClick con credenciales válidas llama a authenticate con los datos correctos`() =
        runTest {
            every { any<String>().isValidEmail() } returns true
            coEvery { mockAccountService.authenticate(any(), any()) } just runs
            coEvery { mockNotificacionService.registrarTokenActual() } just runs

            viewModel.onEmailChange("test@example.com")
            viewModel.onPasswordChange("Password1!")
            viewModel.onSignInClick { _, _ -> }

            advanceUntilIdle() // espera a que terminen las coroutines

            coVerify { mockAccountService.authenticate("test@example.com", "Password1!") }
        }

    @Test
    fun `onSignInClick con credenciales válidas navega a HOME y hace pop de LOGIN`() = runTest {
        every { any<String>().isValidEmail() } returns true
        coEvery { mockAccountService.authenticate(any(), any()) } just runs
        coEvery { mockNotificacionService.registrarTokenActual() } just runs

        var destino = ""
        var popUp = ""
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("Password1!")
        viewModel.onSignInClick { dest, pop ->
            destino = dest
            popUp = pop
        }

        advanceUntilIdle()

        assertEquals(AparkauRoutes.HOME_SCREEN, destino)
        assertEquals(AparkauRoutes.LOGIN_SCREEN, popUp)
    }

    @Test
    fun `onSignInClick válido no deja mensaje de error en el snackbar`() = runTest {
        every { any<String>().isValidEmail() } returns true
        coEvery { mockAccountService.authenticate(any(), any()) } just runs
        coEvery { mockNotificacionService.registrarTokenActual() } just runs

        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("Password1!")
        viewModel.onSignInClick { _, _ -> }

        advanceUntilIdle()

        assertNull(SnackbarManager.snackbarMessages.value)
    }

    // ─── onForgotPasswordClick ────────────────────────────────────────────────

    @Test
    fun `onForgotPasswordClick con email inválido muestra error de email`() {
        every { any<String>().isValidEmail() } returns false
        viewModel.onEmailChange("invalido")

        viewModel.onForgotPasswordClick()

        val mensaje = SnackbarManager.snackbarMessages.value
        assertNotNull(mensaje)
        assertTrue(mensaje is SnackbarMessage.ResourceSnackbar)
        assertEquals(AppText.email_error, (mensaje as SnackbarMessage.ResourceSnackbar).message)
        coVerify(exactly = 0) { mockAccountService.sendRecoveryEmail(any()) }
    }

    @Test
    fun `onForgotPasswordClick con email válido llama a sendRecoveryEmail`() = runTest {
        every { any<String>().isValidEmail() } returns true
        coEvery { mockAccountService.sendRecoveryEmail(any()) } just runs

        viewModel.onEmailChange("test@example.com")
        viewModel.onForgotPasswordClick()

        advanceUntilIdle()

        coVerify { mockAccountService.sendRecoveryEmail("test@example.com") }
    }

    // ─── onSignUpClick ────────────────────────────────────────────────────────

    @Test
    fun `onSignUpClick navega a SIGN_UP y hace pop de LOGIN`() {
        var destino = ""
        var popUp = ""

        viewModel.onSignUpClick { dest, pop ->
            destino = dest
            popUp = pop
        }

        assertEquals(AparkauRoutes.SIGN_UP_SCREEN, destino)
        assertEquals(AparkauRoutes.LOGIN_SCREEN, popUp)
    }
}

