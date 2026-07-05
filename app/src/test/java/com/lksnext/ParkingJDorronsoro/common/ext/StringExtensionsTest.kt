package com.lksnext.ParkingJDorronsoro.common.ext

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios para las funciones de extensión de String.
 *
 * NOTA: isValidEmail() usa android.util.Patterns (API de Android) y no puede
 * testearse aquí sin Robolectric. Se testea indirectamente a través del ViewModel.
 * isValidPassword() y passwordMatches() son Kotlin puro y se testean directamente.
 */
class StringExtensionsTest {

    // ─── isValidPassword ───────────────────────────────────────────────────────

    @Test
    fun `contraseña válida con todos los requisitos devuelve true`() {
        // 8+ chars, mayúscula, minúscula, dígito, especial
        assertTrue("Abcdef1@".isValidPassword())
    }

    @Test
    fun `contraseña sin letra mayúscula devuelve false`() {
        assertFalse("abcdef1@".isValidPassword())
    }

    @Test
    fun `contraseña sin letra minúscula devuelve false`() {
        assertFalse("ABCDEF1@".isValidPassword())
    }

    @Test
    fun `contraseña sin dígito numérico devuelve false`() {
        assertFalse("Abcdefgh@".isValidPassword())
    }

    @Test
    fun `contraseña sin carácter especial devuelve false`() {
        assertFalse("Abcdefg1".isValidPassword())
    }

    @Test
    fun `contraseña con menos de 8 caracteres devuelve false`() {
        assertFalse("Ab1@".isValidPassword())
    }

    @Test
    fun `contraseña vacía devuelve false`() {
        assertFalse("".isValidPassword())
    }

    @Test
    fun `contraseña solo con espacios devuelve false`() {
        assertFalse("        ".isValidPassword())
    }

    // ─── passwordMatches ───────────────────────────────────────────────────────

    @Test
    fun `contraseñas idénticas devuelven true`() {
        assertTrue("MiPass1@".passwordMatches("MiPass1@"))
    }

    @Test
    fun `contraseñas distintas devuelven false`() {
        assertFalse("MiPass1@".passwordMatches("OtroPass1@"))
    }

    @Test
    fun `contraseña original vacía y repetición vacía devuelven true`() {
        assertTrue("".passwordMatches(""))
    }

    @Test
    fun `comparación sensible a mayúsculas devuelve false`() {
        assertFalse("MiPass1@".passwordMatches("mipass1@"))
    }
}

