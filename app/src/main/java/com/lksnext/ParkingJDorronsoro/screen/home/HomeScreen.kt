package com.lksnext.ParkingJDorronsoro.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lksnext.ParkingJDorronsoro.R
import com.lksnext.ParkingJDorronsoro.common.AppText
import com.lksnext.ParkingJDorronsoro.common.composable.BasicButton
import com.lksnext.ParkingJDorronsoro.common.ext.basicButton
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarManager
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarMessage
import com.lksnext.ParkingJDorronsoro.theme.AparkauTheme
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.filterNotNull
import java.text.SimpleDateFormat
import java.util.Locale

private val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

@Composable
fun HomeScreen(
    openScreen: (String) -> Unit,
    openAndPopUp: (String, String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
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
        HomeScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onReserveClick = { viewModel.onReserveClick(openScreen) },
            onMiCuentaClick = { viewModel.onMiCuentaClick(openScreen) },
            onEliminarReservaClick = { viewModel.onEliminarReservaClick(it) },
            onEditarReservaClick = { viewModel.onEditarReservaClick(it, openScreen) },
            onAvisarSalidaClick = { viewModel.onAvisarSalidaClick(it) },
            onAvisoSalidaVistoClick = { viewModel.onAvisoSalidaVistoClick(it) }
        )
    }
}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    onReserveClick: () -> Unit,
    onMiCuentaClick: () -> Unit,
    onEliminarReservaClick: (String) -> Unit = {},
    onEditarReservaClick: (String) -> Unit = {},
    onAvisarSalidaClick: (String) -> Unit = {},
    onAvisoSalidaVistoClick: (String) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(AppText.home_welcome),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.my_reservations),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.reservas.isEmpty()) {
            Text(
                text = stringResource(R.string.no_reservations),
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 4.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.reservas, key = { it.reserva.id }) { reservaUi ->
                    ReservaItem(
                        reservaUi = reservaUi,
                        onEliminarClick = { onEliminarReservaClick(reservaUi.reserva.id) },
                        onEditarClick = { onEditarReservaClick(reservaUi.reserva.id) },
                        onAvisarSalidaClick = { onAvisarSalidaClick(reservaUi.reserva.plazaId) },
                        onAvisoSalidaVistoClick = { onAvisoSalidaVistoClick(reservaUi.reserva.id) }
                    )
                }
            }
        }

        BasicButton(
            text = AppText.reserve_spot,
            modifier = Modifier.basicButton(),
            action = onReserveClick
        )

        BasicButton(
            text = AppText.mi_cuenta_title,
            modifier = Modifier.basicButton(),
            action = onMiCuentaClick
        )
    }
}

@Composable
private fun ReservaItem(
    reservaUi: ReservaHomeUi,
    onEliminarClick: () -> Unit,
    onEditarClick: () -> Unit,
    onAvisarSalidaClick: () -> Unit,
    onAvisoSalidaVistoClick: () -> Unit = {}
) {
    val reserva = reservaUi.reserva
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Cabecera: "Tu reserva de hoy" + chip "Tándem"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (reservaUi.esHoy) {
                        stringResource(R.string.tu_reserva_hoy)
                    } else {
                        formatDay(reserva.fechaReserva ?: reserva.horaInicio)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (reservaUi.esTandem) {
                    TandemChip()
                }
                IconButton(onClick = onEditarClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.editar_reserva),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.eliminar_reserva),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Número de plaza (grande) + zona
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = reserva.plazaId,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                if (reservaUi.zona.isNotBlank()) {
                    Text(
                        text = " ${reservaUi.zona}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            // Horario + matrícula
            Text(
                text = buildString {
                    append(formatTime(reserva.horaInicio))
                    append(" – ")
                    append(formatTime(reserva.horaFin))
                    if (reserva.matriculaVehiculo.isNotBlank()) {
                        append(" · ")
                        append(reserva.matriculaVehiculo)
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )

            // Aviso para el BLOQUEADOR: alguien de su plaza tándem necesita salir.
            if (reservaUi.avisoSalidaPendiente) {
                Spacer(modifier = Modifier.height(12.dp))
                AvisoSalidaBanner(onEntendidoClick = onAvisoSalidaVistoClick)
            }

            // Bloque de bloqueo + botón "Avisar para salir"
            if (reservaUi.bloqueado) {
                Spacer(modifier = Modifier.height(12.dp))
                BloqueadoBanner()
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAvisarSalidaClick,
                    enabled = !reservaUi.avisoEnviado,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(
                            if (reservaUi.avisoEnviado) R.string.aviso_enviado
                            else R.string.avisar_salir
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.avisar_salir_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = stringResource(R.string.eliminar_reserva)) },
            text = { Text(text = stringResource(R.string.eliminar_reserva_confirmacion)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onEliminarClick()
                    }
                ) {
                    Text(text = stringResource(R.string.eliminar))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(R.string.cancelar))
                }
            }
        )
    }
}

@Composable
private fun TandemChip() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = stringResource(R.string.tipo_tandem),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun BloqueadoBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.bloqueado_titulo),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(R.string.bloqueado_subtitulo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Banner ámbar para el BLOQUEADOR: un compañero de su plaza tándem ha pedido
 * salir. Incluye un botón "Entendido" para descartarlo.
 */
@Composable
private fun AvisoSalidaBanner(onEntendidoClick: () -> Unit) {
    val ambar = Color(0xFFFF8F00)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = ambar.copy(alpha = 0.18f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ambar,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.aviso_salida_recibido_titulo),
                        fontWeight = FontWeight.Bold,
                        color = ambar
                    )
                    Text(
                        text = stringResource(R.string.aviso_salida_recibido_subtitulo),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ambar
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEntendidoClick) {
                    Text(
                        text = stringResource(R.string.aviso_salida_entendido),
                        color = ambar,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatDay(timestamp: Timestamp?): String =
    timestamp?.toDate()?.let { dayFormat.format(it) } ?: "-"

private fun formatTime(timestamp: Timestamp?): String =
    timestamp?.toDate()?.let { timeFormat.format(it) } ?: "--:--"

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AparkauTheme {
        HomeScreenContent(
            uiState = HomeUiState(userId = "preview-user"),
            onReserveClick = {},
            onMiCuentaClick = {}
        )
    }
}
