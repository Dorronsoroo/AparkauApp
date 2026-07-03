package com.lksnext.ParkingJDorronsoro.common.composable

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun BasicButton(
    @StringRes text: Int,
    modifier: Modifier = Modifier,
    action: () -> Unit
) {
    Button(
        onClick = action,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Text(text = stringResource(text))
    }
}

@Composable
fun BasicTextButton(
    @StringRes text: Int,
    modifier: Modifier = Modifier,
    action: () -> Unit
) {
    TextButton(onClick = action, modifier = modifier) {
        Text(text = stringResource(text))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicToolbar(@StringRes title: Int) {
    TopAppBar(title = { Text(text = stringResource(title)) })
}

