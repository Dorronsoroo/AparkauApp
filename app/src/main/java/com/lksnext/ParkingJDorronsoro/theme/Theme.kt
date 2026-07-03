package com.lksnext.ParkingJDorronsoro.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary          = OrangeLight,
    onPrimary        = OnOrange,
    primaryContainer = OrangeDark,
    onPrimaryContainer = OnOrange,
    secondary        = NeutralGrey,
    onSecondary      = OnOrange,
    tertiary         = OrangeContainer,
    onTertiary       = OnOrangeContainer
)

private val LightColorScheme = lightColorScheme(
    primary          = OrangePrimary,
    onPrimary        = OnOrange,
    primaryContainer = OrangeContainer,
    onPrimaryContainer = OnOrangeContainer,
    secondary        = NeutralGrey,
    onSecondary      = OnOrange,
    tertiary         = OrangeDark,
    onTertiary       = OnOrange
)

@Composable
fun AparkauTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Color dinámico desactivado para respetar la identidad corporativa naranja
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}