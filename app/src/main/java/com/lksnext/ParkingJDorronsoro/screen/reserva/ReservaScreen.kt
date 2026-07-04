package com.lksnext.ParkingJDorronsoro.screen.reserva

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lksnext.ParkingJDorronsoro.R
import com.lksnext.ParkingJDorronsoro.common.AppText
import com.lksnext.ParkingJDorronsoro.common.composable.BasicToolbar
import com.lksnext.ParkingJDorronsoro.common.ext.fieldModifier
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarManager
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarMessage
import com.lksnext.ParkingJDorronsoro.model.EstadoPlaza
import com.lksnext.ParkingJDorronsoro.model.GrupoPlaza
import com.lksnext.ParkingJDorronsoro.model.Plaza
import com.lksnext.ParkingJDorronsoro.model.TipoPlaza
import com.lksnext.ParkingJDorronsoro.model.Vehiculo
import com.lksnext.ParkingJDorronsoro.theme.AparkauTheme
import com.lksnext.ParkingJDorronsoro.theme.ErrorContainer
import com.lksnext.ParkingJDorronsoro.theme.OnErrorContainer
import com.lksnext.ParkingJDorronsoro.theme.OnSuccessContainer
import com.lksnext.ParkingJDorronsoro.theme.SuccessContainer
import kotlinx.coroutines.flow.filterNotNull
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun ReservaScreen(
    openAndPopUp: (String, String) -> Unit,
    viewModel: ReservaViewModel = hiltViewModel()
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
        ReservaScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onVehiculoSelected = viewModel::onVehiculoSelected,
            onFechaChange = viewModel::onFechaChange,
            onHoraInicioChange = viewModel::onHoraInicioChange,
            onHoraFinChange = viewModel::onHoraFinChange,
            onReservarClick = viewModel::onReservarClick,
            onVolverClick = { viewModel.onVolverClick(openAndPopUp) }
        )
    }
}

@Composable
fun ReservaScreenContent(
    modifier: Modifier = Modifier,
    uiState: ReservaUiState,
    onVehiculoSelected: (String) -> Unit,
    onFechaChange: (LocalDate) -> Unit,
    onHoraInicioChange: (LocalTime) -> Unit,
    onHoraFinChange: (LocalTime) -> Unit,
    onReservarClick: (Plaza) -> Unit,
    onVolverClick: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        BasicToolbar(title = AppText.reservas_title)

        VehiculoSelector(
            vehiculos = uiState.vehiculos,
            matriculaSeleccionada = uiState.matriculaSeleccionada,
            onVehiculoSelected = onVehiculoSelected
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day -> onFechaChange(LocalDate.of(year, month + 1, day)) },
                        uiState.fecha.year,
                        uiState.fecha.monthValue - 1,
                        uiState.fecha.dayOfMonth
                    ).show()
                }
            ) {
                Text(stringResource(R.string.select_date, uiState.fecha.format(dateFormatter)))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> onHoraInicioChange(LocalTime.of(hour, minute)) },
                        uiState.horaInicio.hour,
                        uiState.horaInicio.minute,
                        true
                    ).show()
                }
            ) {
                Text(stringResource(R.string.select_start_time, uiState.horaInicio.format(timeFormatter)))
            }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> onHoraFinChange(LocalTime.of(hour, minute)) },
                        uiState.horaFin.hour,
                        uiState.horaFin.minute,
                        true
                    ).show()
                }
            ) {
                Text(stringResource(R.string.select_end_time, uiState.horaFin.format(timeFormatter)))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.plazas.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(R.string.no_plazas))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.gruposOficina.isNotEmpty()) {
                    item(key = "header_oficina") {
                        SectionHeader(title = stringResource(R.string.garaje_oficina_title))
                    }
                    items(uiState.gruposOficina, key = { grupo ->
                        when (grupo) {
                            is GrupoPlaza.Single -> "of_${grupo.plaza.id}"
                            is GrupoPlaza.Tandem -> "of_tandem_${grupo.plazaA.id}_${grupo.plazaB.id}"
                        }
                    }) { grupo ->
                        GrupoPlazaItem(
                            grupo = grupo,
                            ocupantePorPlaza = uiState.ocupantePorPlaza,
                            onReservarClick = onReservarClick
                        )
                    }
                }

                if (uiState.gruposPago.isNotEmpty()) {
                    item(key = "header_pago") {
                        SectionHeader(title = stringResource(R.string.parking_pago_title))
                    }
                    items(uiState.gruposPago, key = { grupo ->
                        when (grupo) {
                            is GrupoPlaza.Single -> "pg_${grupo.plaza.id}"
                            is GrupoPlaza.Tandem -> "pg_tandem_${grupo.plazaA.id}_${grupo.plazaB.id}"
                        }
                    }) { grupo ->
                        GrupoPlazaItem(
                            grupo = grupo,
                            ocupantePorPlaza = uiState.ocupantePorPlaza,
                            onReservarClick = onReservarClick
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onVolverClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(50.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(text = stringResource(R.string.back_to_home))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehiculoSelector(
    vehiculos: List<Vehiculo>,
    matriculaSeleccionada: String,
    onVehiculoSelected: (String) -> Unit
) {
    if (vehiculos.isEmpty()) {
        Text(
            text = stringResource(R.string.sin_coches_reserva),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        return
    }

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fieldModifier()
    ) {
        OutlinedTextField(
            value = matriculaSeleccionada,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.selecciona_vehiculo)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            vehiculos.forEach { vehiculo ->
                DropdownMenuItem(
                    text = { Text("${vehiculo.matricula} · ${vehiculo.modelo}") },
                    onClick = {
                        onVehiculoSelected(vehiculo.matricula)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun GrupoPlazaItem(
    grupo: GrupoPlaza,
    ocupantePorPlaza: Map<String, String>,
    onReservarClick: (Plaza) -> Unit
) {
    when (grupo) {
        is GrupoPlaza.Single -> PlazaItem(
            plaza = grupo.plaza,
            ocupante = ocupantePorPlaza[grupo.plaza.id],
            onReservarClick = onReservarClick
        )
        is GrupoPlaza.Tandem -> TandemPlazaItem(
            plazaA = grupo.plazaA,
            plazaB = grupo.plazaB,
            ocupantePorPlaza = ocupantePorPlaza,
            onReservarClick = onReservarClick
        )
    }
}

@Composable
private fun TandemPlazaItem(
    plazaA: Plaza,
    plazaB: Plaza,
    ocupantePorPlaza: Map<String, String>,
    onReservarClick: (Plaza) -> Unit
) {
    // Extraemos el prefijo numérico común (p. ej. "36" de "36A" y "36B")
    val prefijo = plazaA.id.trimEnd { !it.isDigit() }

    // Plaza pendiente de confirmar reserva (muestra el diálogo de aviso tándem)
    var plazaAConfirmar by remember { mutableStateOf<Plaza?>(null) }

    plazaAConfirmar?.let { plaza ->
        val esPlazaA = plaza.id == plazaA.id
        val mensaje = if (esPlazaA) {
            stringResource(R.string.dialog_tandem_a_mensaje, plazaA.id, plazaB.id)
        } else {
            stringResource(R.string.dialog_tandem_b_mensaje, plazaB.id, plazaA.id)
        }
        AlertDialog(
            onDismissRequest = { plazaAConfirmar = null },
            title = { Text(text = stringResource(R.string.dialog_tandem_titulo)) },
            text = { Text(text = mensaje) },
            confirmButton = {
                TextButton(onClick = {
                    onReservarClick(plaza)
                    plazaAConfirmar = null
                }) {
                    Text(text = stringResource(R.string.dialog_tandem_confirmar))
                }
            },
            dismissButton = {
                TextButton(onClick = { plazaAConfirmar = null }) {
                    Text(text = stringResource(R.string.dialog_cancelar))
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.plaza_tandem_grupo, prefijo),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.tipo_tandem),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            androidx.compose.material3.HorizontalDivider()

            listOf(plazaA, plazaB).forEach { plaza ->
                val libre = plaza.estadoEnum == EstadoPlaza.LIBRE
                val subCardColor = if (libre) SuccessContainer else ErrorContainer
                val subContentColor = if (libre) OnSuccessContainer else OnErrorContainer
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = subCardColor,
                        contentColor = subContentColor
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.plaza_numero, plaza.id),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = estadoTexto(plaza.estadoEnum),
                                style = MaterialTheme.typography.bodySmall
                            )
                            val ocupante = ocupantePorPlaza[plaza.id]
                            if (!ocupante.isNullOrBlank()) {
                                Text(
                                    text = stringResource(R.string.reservado_por, ocupante),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (plaza.estadoEnum == EstadoPlaza.LIBRE) {
                            Button(onClick = { plazaAConfirmar = plaza }) {
                                Text(text = stringResource(R.string.reservar))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun PlazaItem(
    plaza: Plaza,
    ocupante: String?,
    onReservarClick: (Plaza) -> Unit
) {
    val libre = plaza.estadoEnum == EstadoPlaza.LIBRE
    val cardColor = if (libre) SuccessContainer else ErrorContainer
    val contentColor = if (libre) OnSuccessContainer else OnErrorContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor, contentColor = contentColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.plaza_numero, plaza.id),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${tipoTexto(plaza.tipoEnum)} · ${estadoTexto(plaza.estadoEnum)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!ocupante.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.reservado_por, ocupante),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (plaza.estadoEnum == EstadoPlaza.LIBRE) {
                Button(onClick = { onReservarClick(plaza) }) {
                    Text(text = stringResource(R.string.reservar))
                }
            }
        }
    }
}

@Composable
private fun tipoTexto(tipo: TipoPlaza): String = when (tipo) {
    TipoPlaza.NORMAL -> stringResource(R.string.tipo_normal)
    TipoPlaza.ELECTRICA -> stringResource(R.string.tipo_electrica)
    TipoPlaza.MOTO -> stringResource(R.string.tipo_moto)
    TipoPlaza.PRIORITARIA -> stringResource(R.string.tipo_prioritaria)
    TipoPlaza.TANDEM -> stringResource(R.string.tipo_tandem)
}

@Composable
private fun estadoTexto(estado: EstadoPlaza): String = when (estado) {
    EstadoPlaza.LIBRE -> stringResource(R.string.estado_libre)
    EstadoPlaza.OCUPADA -> stringResource(R.string.estado_ocupada)
    EstadoPlaza.BLOQUEADA_POR_TANDEM -> stringResource(R.string.estado_bloqueada)
}

@Preview(showBackground = true)
@Composable
fun ReservaScreenPreview() {
    AparkauTheme {
        ReservaScreenContent(
            uiState = ReservaUiState(
                plazas = listOf(
                    Plaza(id = "2", tipo = "NORMAL", estado = "LIBRE"),
                    Plaza(id = "11", tipo = "ELECTRICA", estado = "OCUPADA"),
                    Plaza(id = "200", tipo = "MOTO", estado = "LIBRE"),
                    Plaza(id = "63", tipo = "NORMAL", estado = "LIBRE"),
                    Plaza(id = "36A", tipo = "TANDEM", estado = "LIBRE", plazaBloqueadaId = "36B"),
                    Plaza(id = "36B", tipo = "TANDEM", estado = "BLOQUEADA_POR_TANDEM", plazaBloqueadaId = "36A")
                ),
                vehiculos = listOf(
                    Vehiculo(matricula = "1234ABC", modelo = "Seat León")
                ),
                matriculaSeleccionada = "1234ABC"
            ),
            onVehiculoSelected = {},
            onFechaChange = {},
            onHoraInicioChange = {},
            onHoraFinChange = {},
            onReservarClick = {},
            onVolverClick = {}
        )
    }
}

