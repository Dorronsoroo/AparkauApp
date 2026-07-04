package com.lksnext.ParkingJDorronsoro

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lksnext.ParkingJDorronsoro.screen.editar_reserva.EditarReservaScreen
import com.lksnext.ParkingJDorronsoro.screen.home.HomeScreen
import com.lksnext.ParkingJDorronsoro.screen.login.LoginScreen
import com.lksnext.ParkingJDorronsoro.screen.mi_cuenta.MiCuentaScreen
import com.lksnext.ParkingJDorronsoro.screen.mis_coches.MisCochesScreen
import com.lksnext.ParkingJDorronsoro.screen.reserva.ReservaScreen
import com.lksnext.ParkingJDorronsoro.screen.sign_up.SignUpScreen

@Composable
fun AparkauNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AparkauRoutes.LOGIN_SCREEN
    ) {
        signUpGraph(navController)
        loginGraph(navController)

        composable(route = AparkauRoutes.HOME_SCREEN) {
            HomeScreen(
                openScreen = { route -> navController.navigate(route) },
                openAndPopUp = { route, popUp ->
                    navController.navigate(route) {
                        popUpTo(popUp) { inclusive = true }
                    }
                }
            )
        }

        composable(route = AparkauRoutes.RESERVA_SCREEN) {
            ReservaScreen(
                openAndPopUp = { route, popUp ->
                    navController.navigate(route) {
                        popUpTo(popUp) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "${AparkauRoutes.EDITAR_RESERVA_SCREEN}/{reservaId}",
            arguments = listOf(navArgument("reservaId") { type = NavType.StringType })
        ) {
            EditarReservaScreen(
                openAndPopUp = { route, popUp ->
                    navController.navigate(route) {
                        popUpTo(popUp) { inclusive = true }
                    }
                }
            )
        }

        composable(route = AparkauRoutes.MIS_COCHES_SCREEN) {
            MisCochesScreen(
                openAndPopUp = { route, popUp ->
                    navController.navigate(route) {
                        popUpTo(popUp) { inclusive = true }
                    }
                }
            )
        }

        composable(route = AparkauRoutes.MI_CUENTA_SCREEN) {
            MiCuentaScreen(
                openScreen = { route -> navController.navigate(route) },
                openAndPopUp = { route, popUp ->
                    navController.navigate(route) {
                        popUpTo(popUp) { inclusive = true }
                    }
                }
            )
        }
    }
}

private fun NavGraphBuilder.signUpGraph(navController: NavHostController) {
    composable(route = AparkauRoutes.SIGN_UP_SCREEN) {
        SignUpScreen(
            openAndPopUp = { route, popUp ->
                navController.navigate(route) {
                    popUpTo(popUp) { inclusive = true }
                }
            }
        )
    }
}

private fun NavGraphBuilder.loginGraph(navController: NavHostController) {
    composable(route = AparkauRoutes.LOGIN_SCREEN) {
        LoginScreen(
            openAndPopUp = { route, popUp ->
                navController.navigate(route) {
                    popUpTo(popUp) { inclusive = true }
                }
            }
        )
    }
}
