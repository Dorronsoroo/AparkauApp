package com.lksnext.ParkingJDorronsoro.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Formas redondeadas consistentes en toda la app.
// small  -> campos de texto, chips
// medium -> botones, contenedores pequeños
// large  -> tarjetas, diálogos, banners
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
