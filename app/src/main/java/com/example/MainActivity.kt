package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.MainViewModel
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.CardioNeuroTheme
import com.example.ui.theme.Spacing

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val isDark by viewModel.isDarkTheme.collectAsState()
            CardioNeuroTheme(darkTheme = isDark) {
                CardioNeuroAppNavHost(viewModel = viewModel)
            }
        }
    }
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardioNeuroAppNavHost(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()

    val navItems = listOf(
        BottomNavItem("Dashboard", Screen.Dashboard.route, Icons.Default.Home, "bottom_nav_home"),
        BottomNavItem("Patients", Screen.PatientList.route, Icons.Default.People, "bottom_nav_patients"),
        BottomNavItem("Predict", Screen.NewDiagnostic.createRoute(1L), Icons.Default.Psychology, "bottom_nav_predict"),
        BottomNavItem("Settings", Screen.FederatedPrivacy.route, Icons.Default.Settings, "bottom_nav_settings")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBottomBar = when {
        currentRoute == Screen.Dashboard.route -> true
        currentRoute == Screen.PatientList.route -> true
        currentRoute?.startsWith("new_diagnostic") == true -> true
        currentRoute == Screen.FederatedPrivacy.route -> true
        else -> false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.height(64.dp)
                ) {
                    navItems.forEach { item ->
                        val isSelected = when (item.title) {
                            "Dashboard" -> currentRoute == Screen.Dashboard.route
                            "Patients" -> currentRoute == Screen.PatientList.route
                            "Predict" -> currentRoute?.startsWith("new_diagnostic") == true
                            "Settings" -> currentRoute == Screen.FederatedPrivacy.route
                            else -> false
                        }

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .testTag(item.testTag)
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToPatientList = {
                        navController.navigate(Screen.PatientList.route)
                    },
                    onNavigateToPatientDetail = { patientId ->
                        navController.navigate(Screen.PatientDetail.createRoute(patientId))
                    },
                    onNavigateToPredictionDetail = { predId ->
                        navController.navigate(Screen.PredictionDetail.createRoute(predId))
                    },
                    onNavigateToFederatedPrivacy = {
                        navController.navigate(Screen.FederatedPrivacy.route)
                    },
                    onNavigateToAnalytics = {
                        navController.navigate(Screen.Analytics.route)
                    }
                )
            }

            composable(Screen.PatientList.route) {
                PatientListScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToPatientDetail = { patientId ->
                        navController.navigate(Screen.PatientDetail.createRoute(patientId))
                    },
                    onNavigateToNewDiagnostic = { patientId ->
                        navController.navigate(Screen.NewDiagnostic.createRoute(patientId))
                    }
                )
            }

            composable(
                route = Screen.PatientDetail.route,
                arguments = listOf(navArgument("patientId") { type = NavType.LongType })
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getLong("patientId") ?: 1L
                PatientDetailScreen(
                    patientId = patientId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToNewDiagnostic = { pId ->
                        navController.navigate(Screen.NewDiagnostic.createRoute(pId))
                    },
                    onNavigateToPredictionDetail = { predId ->
                        navController.navigate(Screen.PredictionDetail.createRoute(predId))
                    }
                )
            }

            composable(
                route = Screen.NewDiagnostic.route,
                arguments = listOf(navArgument("patientId") { type = NavType.LongType })
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getLong("patientId") ?: 1L
                NewDiagnosticScreen(
                    patientId = patientId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onPredictionComplete = { predId ->
                        navController.navigate(Screen.PredictionDetail.createRoute(predId)) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
            }

            composable(
                route = Screen.PredictionDetail.route,
                arguments = listOf(navArgument("predictionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val predId = backStackEntry.arguments?.getLong("predictionId") ?: 1L
                PredictionDetailScreen(
                    predictionId = predId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.FederatedPrivacy.route) {
                FederatedPrivacyScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToModelEvaluation = {
                        navController.navigate(Screen.ModelEvaluation.route)
                    }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ModelEvaluation.route) {
                ModelEvaluationScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
