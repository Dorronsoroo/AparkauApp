package com.lksnext.ParkingJDorronsoro.screen.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Tests unitarios para el data class LoginUiState.
 *
 * Al ser un data class de Kotlin, obtenemos gratis equals(), copy() y toString().
 * Estos tests verifican que el estado se comporta correctamente.
 */
class LoginUiStateTest {

    @Test
    fun `estado inicial tiene email y contraseña vacíos`() {
        val state = LoginUiState()

        assertEquals("", state.email)
        assertEquals("", state.password)
    }

    @Test
    fun `copy con nuevo email no modifica la contraseña`() {
        val original = LoginUiState(email = "viejo@test.com", password = "Pass1@xyz")
        val actualizado = original.copy(email = "nuevo@test.com")

        assertEquals("nuevo@test.com", actualizado.email)
        assertEquals("Pass1@xyz", actualizado.password)  // sin cambios
    }

    @Test
    fun `copy con nueva contraseña no modifica el email`() {
        val original = LoginUiState(email = "test@test.com", password = "OldPass1@")
        val actualizado = original.copy(password = "NewPass1@")

        assertEquals("test@test.com", actualizado.email)  // sin cambios
        assertEquals("NewPass1@", actualizado.password)
    }

    @Test
    fun `dos instancias con los mismos valores son iguales`() {
        val state1 = LoginUiState(email = "a@b.com", password = "Pass1@")
        val state2 = LoginUiState(email = "a@b.com", password = "Pass1@")

        assertEquals(state1, state2)
    }

    @Test
    fun `dos instancias con email diferente no son iguales`() {
        val state1 = LoginUiState(email = "a@b.com", password = "Pass1@")
        val state2 = LoginUiState(email = "x@y.com", password = "Pass1@")

        assertNotEquals(state1, state2)
    }
}

