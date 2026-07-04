package com.lksnext.ParkingJDorronsoro.screen.mis_coches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lksnext.ParkingJDorronsoro.R
import com.lksnext.ParkingJDorronsoro.common.AppText
import com.lksnext.ParkingJDorronsoro.common.composable.BasicButton
import com.lksnext.ParkingJDorronsoro.common.composable.BasicToolbar
import com.lksnext.ParkingJDorronsoro.common.ext.basicButton
import com.lksnext.ParkingJDorronsoro.common.ext.fieldModifier
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarManager
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarMessage
import com.lksnext.ParkingJDorronsoro.model.Vehiculo
import com.lksnext.ParkingJDorronsoro.theme.AparkauTheme
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun MisCochesScreen(
    openAndPopUp: (String, String) -> Unit,
    viewModel: MisCochesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        SnackbarManager.snackbarMessages.filterNotNull().collect { message ->
            val text = when (message) {
                is SnackbarMessage.ResourceSnackbar -> context.getString(message.message)
                is SnackbarMessage.StringSnackbar -> message.message
            }
            snackbarHostState.showSnackbar(text)
            SnackbarManager.clearSnackbarState()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        }
    ) { paddingValues ->
        MisCochesScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onMatriculaChange = viewModel::onMatriculaChange,
            onModeloChange = viewModel::onModeloChange,
            onAddVehiculoClick = viewModel::onAddVehiculoClick,
            onEliminarVehiculoClick = viewModel::onEliminarVehiculoClick,
            onVolverClick = { viewModel.onVolverClick(openAndPopUp) }
        )
    }
}

@Composable
fun MisCochesScreenContent(
    modifier: Modifier = Modifier,
    uiState: MisCochesUiState,
    onMatriculaChange: (String) -> Unit,
    onModeloChange: (String) -> Unit,
    onAddVehiculoClick: () -> Unit,
    onEliminarVehiculoClick: (Vehiculo) -> Unit,
    onVolverClick: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        BasicToolbar(title = AppText.mis_coches_title)

        OutlinedTextField(
            value = uiState.matricula,
            onValueChange = onMatriculaChange,
            singleLine = true,
            modifier = Modifier.fieldModifier(),
            label = { Text(stringResource(R.string.matricula)) },
            shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
        )

        OutlinedTextField(
            value = uiState.modelo,
            onValueChange = onModeloChange,
            singleLine = true,
            modifier = Modifier.fieldModifier(),
            label = { Text(stringResource(R.string.modelo)) },
            shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        BasicButton(
            text = AppText.add_vehiculo,
            modifier = Modifier.basicButton(),
            action = onAddVehiculoClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.mis_coches_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.vehiculos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(R.string.no_vehiculos))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 4.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.vehiculos, key = { it.matricula }) { vehiculo ->
                    VehiculoItem(vehiculo = vehiculo, onEliminarClick = onEliminarVehiculoClick)
                }
            }
        }

        OutlinedButton(
            onClick = onVolverClick,
            modifier = Modifier
                .basicButton()
                .fillMaxWidth()
                .height(54.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(text = stringResource(AppText.back_to_home))
        }
    }
}

@Composable
private fun VehiculoItem(
    vehiculo: Vehiculo,
    onEliminarClick: (Vehiculo) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vehiculo.matricula,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = vehiculo.modelo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { onEliminarClick(vehiculo) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.eliminar),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MisCochesScreenPreview() {
    AparkauTheme {
        MisCochesScreenContent(
            uiState = MisCochesUiState(
                vehiculos = listOf(
                    Vehiculo(matricula = "1234ABC", modelo = "Seat León"),
                    Vehiculo(matricula = "5678XYZ", modelo = "Tesla Model 3")
                )
            ),
            onMatriculaChange = {},
            onModeloChange = {},
            onAddVehiculoClick = {},
            onEliminarVehiculoClick = {},
            onVolverClick = {}
        )
    }
}

