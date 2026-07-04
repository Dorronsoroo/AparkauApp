package com.lksnext.ParkingJDorronsoro.screen.editar_reserva

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
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
import kotlinx.coroutines.flow.filterNotNull
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun EditarReservaScreen(
    openAndPopUp: (String, String) -> Unit,
    viewModel: EditarReservaViewModel = hiltViewModel()
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
        EditarReservaScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onVehiculoSelected = viewModel::onVehiculoSelected,
            onFechaChange = viewModel::onFechaChange,
            onHoraInicioChange = viewModel::onHoraInicioChange,
            onHoraFinChange = viewModel::onHoraFinChange,
            onPlazaSeleccionada = viewModel::onPlazaSeleccionadaChange,
            onActualizarClick = { viewModel.onActualizarClick(openAndPopUp) },
            onVolverClick = { viewModel.onVolverClick(openAndPopUp) }
        )
    }
}

@Composable
fun EditarReservaScreenContent(
    modifier: Modifier = Modifier,
    uiState: EditarReservaUiState,
    onVehiculoSelected: (String) -> Unit,
    onFechaChange: (LocalDate) -> Unit,
    onHoraInicioChange: (LocalTime) -> Unit,
    onHoraFinChange: (LocalTime) -> Unit,
    onPlazaSeleccionada: (String) -> Unit,
    onActualizarClick: () -> Unit,
    onVolverClick: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        BasicToolbar(title = AppText.editar_reserva_title)

        // Selector de vehículo
        VehiculoSelectorEditar(
            vehiculos = uiState.vehiculos,
            matriculaSeleccionada = uiState.matriculaSeleccionada,
            onVehiculoSelected = onVehiculoSelected
        )

        // Selector de fecha
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

        // Selectores de hora
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

        // Resumen plaza seleccionada
        if (uiState.plazaSeleccionadaId.isNotBlank()) {
            Text(
                text = stringResource(R.string.plaza_seleccionada, uiState.plazaSeleccionadaId),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

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
                        SectionHeaderEditar(title = stringResource(R.string.garaje_oficina_title))
                    }
                    items(uiState.gruposOficina, key = { grupo ->
                        when (grupo) {
                            is GrupoPlaza.Single -> "of_${grupo.plaza.id}"
                            is GrupoPlaza.Tandem -> "of_tandem_${grupo.plazaA.id}_${grupo.plazaB.id}"
                        }
                    }) { grupo ->
                        GrupoPlazaItemEditar(
                            grupo = grupo,
                            plazaSeleccionadaId = uiState.plazaSeleccionadaId,
                            ocupantePorPlaza = uiState.ocupantePorPlaza,
                            onSeleccionarClick = onPlazaSeleccionada
                        )
                    }
                }

                if (uiState.gruposPago.isNotEmpty()) {
                    item(key = "header_pago") {
                        SectionHeaderEditar(title = stringResource(R.string.parking_pago_title))
                    }
                    items(uiState.gruposPago, key = { grupo ->
                        when (grupo) {
                            is GrupoPlaza.Single -> "pg_${grupo.plaza.id}"
                            is GrupoPlaza.Tandem -> "pg_tandem_${grupo.plazaA.id}_${grupo.plazaB.id}"
                        }
                    }) { grupo ->
                        GrupoPlazaItemEditar(
                            grupo = grupo,
                            plazaSeleccionadaId = uiState.plazaSeleccionadaId,
                            ocupantePorPlaza = uiState.ocupantePorPlaza,
                            onSeleccionarClick = onPlazaSeleccionada
                        )
                    }
                }
            }
        }

        // Botón actualizar
        Button(
            onClick = onActualizarClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(text = stringResource(R.string.actualizar_reserva), fontWeight = FontWeight.Bold)
        }

        // Botón volver
        OutlinedButton(
            onClick = onVolverClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text(text = stringResource(R.string.back_to_home))
        }
    }
}

@Composable
private fun SectionHeaderEditar(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun GrupoPlazaItemEditar(
    grupo: GrupoPlaza,
    plazaSeleccionadaId: String,
    ocupantePorPlaza: Map<String, String>,
    onSeleccionarClick: (String) -> Unit
) {
    when (grupo) {
        is GrupoPlaza.Single -> PlazaItemEditar(
            plaza = grupo.plaza,
            plazaSeleccionadaId = plazaSeleccionadaId,
            ocupante = ocupantePorPlaza[grupo.plaza.id],
            onSeleccionarClick = onSeleccionarClick
        )
        is GrupoPlaza.Tandem -> TandemPlazaItemEditar(
            plazaA = grupo.plazaA,
            plazaB = grupo.plazaB,
            plazaSeleccionadaId = plazaSeleccionadaId,
            ocupantePorPlaza = ocupantePorPlaza,
            onSeleccionarClick = onSeleccionarClick
        )
    }
}

@Composable
private fun PlazaItemEditar(
    plaza: Plaza,
    plazaSeleccionadaId: String,
    ocupante: String?,
    onSeleccionarClick: (String) -> Unit
) {
    val esSeleccionada = plaza.id == plazaSeleccionadaId
    val cardColor = when {
        esSeleccionada -> Color(0xFF4CAF50)
        plaza.estadoEnum == EstadoPlaza.LIBRE -> Color(0xFFD4EDDA)
        else -> Color(0xFFF8D7DA)
    }
    val border = if (esSeleccionada) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = border
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
                    text = "${tipoTextoEditar(plaza.tipoEnum)} · ${estadoTextoEditar(plaza.estadoEnum)}",
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

            when {
                esSeleccionada -> Text(
                    text = stringResource(R.string.plaza_seleccionada_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                plaza.estadoEnum == EstadoPlaza.LIBRE -> Button(
                    onClick = { onSeleccionarClick(plaza.id) }
                ) {
                    Text(text = stringResource(R.string.seleccionar_plaza))
                }
            }
        }
    }
}

@Composable
private fun TandemPlazaItemEditar(
    plazaA: Plaza,
    plazaB: Plaza,
    plazaSeleccionadaId: String,
    ocupantePorPlaza: Map<String, String>,
    onSeleccionarClick: (String) -> Unit
) {
    val prefijo = plazaA.id.trimEnd { !it.isDigit() }
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
                    onSeleccionarClick(plaza.id)
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
                val esSeleccionada = plaza.id == plazaSeleccionadaId
                val subCardColor = when {
                    esSeleccionada -> Color(0xFF4CAF50)
                    plaza.estadoEnum == EstadoPlaza.LIBRE -> Color(0xFFD4EDDA)
                    else -> Color(0xFFF8D7DA)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = subCardColor)
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
                                text = estadoTextoEditar(plaza.estadoEnum),
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
                        when {
                            esSeleccionada -> Text(
                                text = stringResource(R.string.plaza_seleccionada_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            plaza.estadoEnum == EstadoPlaza.LIBRE -> Button(
                                onClick = { plazaAConfirmar = plaza }
                            ) {
                                Text(text = stringResource(R.string.seleccionar_plaza))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehiculoSelectorEditar(
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
private fun tipoTextoEditar(tipo: TipoPlaza): String = when (tipo) {
    TipoPlaza.NORMAL -> stringResource(R.string.tipo_normal)
    TipoPlaza.ELECTRICA -> stringResource(R.string.tipo_electrica)
    TipoPlaza.MOTO -> stringResource(R.string.tipo_moto)
    TipoPlaza.PRIORITARIA -> stringResource(R.string.tipo_prioritaria)
    TipoPlaza.TANDEM -> stringResource(R.string.tipo_tandem)
}

@Composable
private fun estadoTextoEditar(estado: EstadoPlaza): String = when (estado) {
    EstadoPlaza.LIBRE -> stringResource(R.string.estado_libre)
    EstadoPlaza.OCUPADA -> stringResource(R.string.estado_ocupada)
    EstadoPlaza.BLOQUEADA_POR_TANDEM -> stringResource(R.string.estado_bloqueada)
}

@Preview(showBackground = true)
@Composable
fun EditarReservaScreenPreview() {
    AparkauTheme {
        EditarReservaScreenContent(
            uiState = EditarReservaUiState(
                reservaId = "abc123",
                plazaSeleccionadaId = "2",
                plazas = listOf(
                    Plaza(id = "2", tipo = "NORMAL", estado = "LIBRE"),
                    Plaza(id = "11", tipo = "ELECTRICA", estado = "OCUPADA")
                ),
                vehiculos = listOf(Vehiculo(matricula = "1234ABC", modelo = "Seat León")),
                matriculaSeleccionada = "1234ABC"
            ),
            onVehiculoSelected = {},
            onFechaChange = {},
            onHoraInicioChange = {},
            onHoraFinChange = {},
            onPlazaSeleccionada = {},
            onActualizarClick = {},
            onVolverClick = {}
        )
    }
}

