package com.orbit.mobile.feature.shell

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.orbit.mobile.feature.auth.LoginScreen
import com.orbit.mobile.feature.auth.SetupScreen
import com.orbit.mobile.feature.auth.SplashScreen
import com.orbit.mobile.feature.onboarding.OnboardingScreen

// Route names
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val SETUP = "setup"
    const val LOGIN = "login"
    const val HOME = "home"
}

// App graph
@Composable
fun OrbitNavHost(navController: NavHostController = rememberNavController()) {

    // Reset stack
    fun goRoot(route: String) {
        navController.navigate(route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onNeedsSetup = { goRoot(Routes.SETUP) },
                onReady = { loggedIn, onboardingSeen, _ ->
                    goRoot(
                        when {
                            loggedIn -> Routes.HOME
                            !onboardingSeen -> Routes.ONBOARDING
                            else -> Routes.LOGIN
                        }
                    )
                }
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinish = { goRoot(Routes.LOGIN) })
        }

        composable(Routes.SETUP) {
            SetupScreen(onGoLogin = { goRoot(Routes.LOGIN) })
        }

        composable(Routes.LOGIN) {
            LoginScreen(onLoggedIn = { goRoot(Routes.HOME) })
        }

        composable(Routes.HOME) {
            ShellScreen(onLoggedOut = { goRoot(Routes.LOGIN) })
        }
    }
}
