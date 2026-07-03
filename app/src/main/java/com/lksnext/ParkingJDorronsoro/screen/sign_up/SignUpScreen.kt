package com.lksnext.ParkingJDorronsoro.screen.sign_up

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lksnext.ParkingJDorronsoro.R
import com.lksnext.ParkingJDorronsoro.common.composable.BasicButton
import com.lksnext.ParkingJDorronsoro.common.composable.BasicTextButton
import com.lksnext.ParkingJDorronsoro.common.composable.BasicField
import com.lksnext.ParkingJDorronsoro.common.composable.EmailField
import com.lksnext.ParkingJDorronsoro.common.composable.PasswordField
import com.lksnext.ParkingJDorronsoro.common.ext.basicButton
import com.lksnext.ParkingJDorronsoro.common.ext.fieldModifier
import com.lksnext.ParkingJDorronsoro.common.ext.textButton
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarManager
import com.lksnext.ParkingJDorronsoro.common.snackbar.SnackbarMessage
import com.lksnext.ParkingJDorronsoro.theme.AparkauTheme
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun SignUpScreen(
    openAndPopUp: (String, String) -> Unit,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Observar mensajes del SnackbarManager y mostrarlos
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
        SignUpScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onNombreChange = viewModel::onNombreChange,
            onApellidosChange = viewModel::onApellidosChange,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onRepeatPasswordChange = viewModel::onRepeatPasswordChange,
            onSignUpClick = { viewModel.onSignUpClick(openAndPopUp) },
            onLoginClick = { viewModel.onLoginClick(openAndPopUp) }
        )
    }
}

@Composable
fun SignUpScreenContent(
    modifier: Modifier = Modifier,
    uiState: SignUpUiState,
    onNombreChange: (String) -> Unit,
    onApellidosChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRepeatPasswordChange: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onLoginClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.sign_up),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        BasicField(
            value = uiState.nombre,
            onNewValue = onNombreChange,
            modifier = Modifier.fieldModifier(),
            label = R.string.nombre
        )

        BasicField(
            value = uiState.apellidos,
            onNewValue = onApellidosChange,
            modifier = Modifier.fieldModifier(),
            label = R.string.apellidos
        )

        EmailField(
            value = uiState.email,
            onNewValue = onEmailChange,
            modifier = Modifier.fieldModifier(),
            label = R.string.email
        )

        PasswordField(
            value = uiState.password,
            onNewValue = onPasswordChange,
            modifier = Modifier.fieldModifier(),
            label = R.string.password
        )

        PasswordField(
            value = uiState.repeatPassword,
            onNewValue = onRepeatPasswordChange,
            modifier = Modifier.fieldModifier(),
            label = R.string.repeat_password
        )

        Spacer(modifier = Modifier.height(16.dp))

        BasicButton(
            text = R.string.sign_up,
            modifier = Modifier.basicButton(),
            action = onSignUpClick
        )

        BasicTextButton(
            text = R.string.already_have_account,
            modifier = Modifier.textButton(),
            action = onLoginClick
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    AparkauTheme {
        SignUpScreenContent(
            uiState = SignUpUiState(),
            onNombreChange = {},
            onApellidosChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onRepeatPasswordChange = {},
            onSignUpClick = {}
        )
    }
}