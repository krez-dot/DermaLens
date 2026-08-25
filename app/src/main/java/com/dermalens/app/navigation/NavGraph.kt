package com.dermalens.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dermalens.app.ui.screens.*

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object VerifyEmail : Screen("verify_email")
    object Home : Screen("home")
    object Scan : Screen("scan")
    object ScanResult : Screen("scan_result?imageUri={imageUri}") {
        fun createRoute(imageUri: String?) =
            if (imageUri != null) "scan_result?imageUri=${android.net.Uri.encode(imageUri)}" else "scan_result"
    }
    object CareGuide : Screen("care_guide")
    object ProgressTracker : Screen("progress_tracker")
    object ClinicLocator : Screen("clinic_locator")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
}

@Composable
fun DermaLensNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(navController = navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }
        composable(Screen.VerifyEmail.route) {
            VerifyEmailScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Scan.route) {
            ScanScreen(navController = navController)
        }
        composable(
            Screen.ScanResult.route,
            arguments = listOf(navArgument("imageUri") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            ScanResultScreen(
                navController = navController,
                imageUri = backStackEntry.arguments?.getString("imageUri")
            )
        }
        composable(Screen.CareGuide.route) {
            CareGuideScreen(navController = navController)
        }
        composable(Screen.ProgressTracker.route) {
            ProgressTrackerScreen(navController = navController)
        }
        composable(Screen.ClinicLocator.route) {
            ClinicLocatorScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        composable(Screen.EditProfile.route) {
            EditProfileScreen(navController = navController)
        }
    }
}