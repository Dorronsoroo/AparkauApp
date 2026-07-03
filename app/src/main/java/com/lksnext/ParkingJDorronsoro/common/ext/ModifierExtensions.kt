package com.lksnext.ParkingJDorronsoro.common.ext

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun Modifier.fieldModifier(): Modifier {
    return this
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
}

fun Modifier.basicButton(): Modifier {
    return this.padding(horizontal = 16.dp, vertical = 8.dp)
}

fun Modifier.textButton(): Modifier {
    return this.padding(horizontal = 16.dp, vertical = 4.dp)
}

fun Modifier.card(): Modifier {
    return this.padding(16.dp)
}

