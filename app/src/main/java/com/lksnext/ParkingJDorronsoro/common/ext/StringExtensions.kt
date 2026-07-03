package com.lksnext.ParkingJDorronsoro.common.ext

private const val MIN_PASS_LENGTH = 8
private const val PASS_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{$MIN_PASS_LENGTH,}$"

fun String.isValidEmail(): Boolean {
    return this.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isValidPassword(): Boolean {
    return this.isNotBlank() && this.matches(Regex(PASS_PATTERN))
}

fun String.passwordMatches(repeated: String): Boolean {
    return this == repeated
}

