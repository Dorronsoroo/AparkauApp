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
    primary            = OrangeLight,
    onPrimary          = OnOrangeDarkText,
    primaryContainer   = OrangeDark,
    onPrimaryContainer = OrangeContainer,
    secondary          = NeutralGreyLight,
    onSecondary        = SurfaceVariantDark,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = OnSurfaceVariantDark,
    tertiary           = OrangeLight,
    onTertiary         = OnOrangeDarkText,
    background         = BackgroundDark,
    onBackground       = OnBackgroundDark,
    surface            = SurfaceDark,
    onSurface          = OnSurfaceDark,
    surfaceVariant     = SurfaceVariantDark,
    onSurfaceVariant   = OnSurfaceVariantDark,
    outline            = OutlineDark,
    outlineVariant     = OutlineVariantDark,
    error              = ErrorRed,
    onError            = OnOrange,
    errorContainer     = ErrorContainer,
    onErrorContainer   = OnErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary            = OrangePrimary,
    onPrimary          = OnOrange,
    primaryContainer   = OrangeContainer,
    onPrimaryContainer = OnOrangeContainer,
    secondary          = NeutralGrey,
    onSecondary        = OnNeutral,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = OnSurfaceVariantLight,
    tertiary           = OrangeDark,
    onTertiary         = OnOrange,
    background         = BackgroundLight,
    onBackground       = OnBackgroundLight,
    surface            = SurfaceLight,
    onSurface          = OnSurfaceLight,
    surfaceVariant     = SurfaceVariantLight,
    onSurfaceVariant   = OnSurfaceVariantLight,
    outline            = OutlineLight,
    outlineVariant     = OutlineVariantLight,
    error              = ErrorRed,
    onError            = OnOrange,
    errorContainer     = ErrorContainer,
    onErrorContainer   = OnErrorContainer
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
        shapes = Shapes,
        content = content
    )
}
