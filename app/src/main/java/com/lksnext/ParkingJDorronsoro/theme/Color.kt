package com.lksnext.ParkingJDorronsoro.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Paleta naranja corporativa Aparkau
// ---------------------------------------------------------------------------
val OrangePrimary     = Color(0xFFF5620A)   // Naranja intenso — principal
val OrangeLight       = Color(0xFFFF9E4D)   // Naranja claro — variante light
val OrangeDark        = Color(0xFFC64600)   // Naranja oscuro — variante dark
val OrangeContainer   = Color(0xFFFFE3CC)   // Fondo suave naranja (contenedor)

val OnOrange          = Color(0xFFFFFFFF)   // Texto/icono sobre naranja
val OnOrangeContainer = Color(0xFF7A2E00)   // Texto sobre fondo suave naranja
val OnOrangeDarkText  = Color(0xFF3A1600)   // Texto oscuro sobre naranja claro (tema oscuro)

// Secundario neutro cálido (piedra)
val NeutralGrey       = Color(0xFF6F6862)
val NeutralGreyLight  = Color(0xFFE7E1DB)
val OnNeutral         = Color(0xFFFFFFFF)

// ---------------------------------------------------------------------------
// Superficies y fondos — tema claro
// ---------------------------------------------------------------------------
val BackgroundLight       = Color(0xFFFBF8F5)   // Fondo general cálido, casi blanco
val SurfaceLight          = Color(0xFFFFFFFF)   // Tarjetas / superficies
val SurfaceVariantLight   = Color(0xFFF2EBE4)   // Chips / campos / contenedores suaves
val OnBackgroundLight     = Color(0xFF201B17)   // Texto principal
val OnSurfaceLight        = Color(0xFF201B17)
val OnSurfaceVariantLight = Color(0xFF6F6862)   // Texto secundario / captions
val OutlineLight          = Color(0xFFD8CFC7)   // Bordes / divisores
val OutlineVariantLight   = Color(0xFFEBE3DC)

// ---------------------------------------------------------------------------
// Superficies y fondos — tema oscuro
// ---------------------------------------------------------------------------
val BackgroundDark        = Color(0xFF16130F)
val SurfaceDark           = Color(0xFF221E19)
val SurfaceVariantDark    = Color(0xFF2E2822)
val OnBackgroundDark      = Color(0xFFECE3DB)
val OnSurfaceDark         = Color(0xFFECE3DB)
val OnSurfaceVariantDark  = Color(0xFFCFC4B9)
val OutlineDark           = Color(0xFF4A423B)
val OutlineVariantDark    = Color(0xFF332D27)

// ---------------------------------------------------------------------------
// Colores semánticos de estado (centralizados; antes hardcodeados por pantalla)
// ---------------------------------------------------------------------------
// Éxito / plaza libre
val SuccessGreen        = Color(0xFF2E7D32)
val SuccessContainer    = Color(0xFFD7EED9)
val OnSuccessContainer  = Color(0xFF14401A)

// Error / plaza ocupada o bloqueada
val ErrorRed            = Color(0xFFC62828)
val ErrorContainer      = Color(0xFFF9DEDC)
val OnErrorContainer    = Color(0xFF410E0B)

// Aviso / petición de salida
val WarningAmber        = Color(0xFFE08600)
val WarningContainer    = Color(0xFFFFECC7)
val OnWarningContainer  = Color(0xFF4A2E00)
