package com.lksnext.ParkingJDorronsoro.screen.login

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lksnext.ParkingJDorronsoro.common.AppText
import com.lksnext.ParkingJDorronsoro.common.composable.BasicButton
import com.lksnext.ParkingJDorronsoro.common.composable.BasicTextButton
import com.lksnext.ParkingJDorronsoro.common.composable.BasicToolbar
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
fun LoginScreen(
    openAndPopUp: (String, String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
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
        LoginScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onSignInClick = { viewModel.onSignInClick(openAndPopUp) },
            onForgotPasswordClick = viewModel::onForgotPasswordClick,
            onSignUpClick = { viewModel.onSignUpClick(openAndPopUp) }
        )
    }
}

@Composable
fun LoginScreenContent(
    modifier: Modifier = Modifier,
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicToolbar(title = AppText.login_details)

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EmailField(
                value = uiState.email,
                onNewValue = onEmailChange,
                modifier = Modifier.fieldModifier(),
                label = com.lksnext.ParkingJDorronsoro.R.string.email
            )

            PasswordField(
                value = uiState.password,
                onNewValue = onPasswordChange,
                modifier = Modifier.fieldModifier(),
                label = com.lksnext.ParkingJDorronsoro.R.string.password
            )

            Spacer(modifier = Modifier.height(8.dp))

            BasicButton(
                text = AppText.sign_in,
                modifier = Modifier.basicButton(),
                action = onSignInClick
            )

            BasicTextButton(
                text = AppText.forgot_password,
                modifier = Modifier.textButton(),
                action = onForgotPasswordClick
            )

            BasicTextButton(
                text = AppText.no_account,
                modifier = Modifier.textButton(),
                action = onSignUpClick
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    AparkauTheme {
        LoginScreenContent(
            uiState = LoginUiState(email = "email@test.com"),
            onEmailChange = {},
            onPasswordChange = {},
            onSignInClick = {},
            onForgotPasswordClick = {}
        )
    }
}