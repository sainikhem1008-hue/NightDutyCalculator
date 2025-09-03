package com.khem.nightdutycalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.khem.nightdutycalculator.ui.theme.NightDutyCalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NightDutyCalculatorTheme {
                val navController = rememberNavController()
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(navController)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "calculator"
    ) {
        composable("calculator") {
            CalculatorScreen(
                onNavigateToLeave = { navController.navigate("leave") }
            )
        }
        composable("leave") {
            LeaveScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun CalculatorScreen(onNavigateToLeave: () -> Unit) {
    Scaffold { padding ->
        // TODO: Replace with real UI
        Text(text = "Calculator Screen (NDA)", modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun LeaveScreen(onBack: () -> Unit) {
    Scaffold { padding ->
        // TODO: Replace with real UI
        Text(text = "Leave Form Screen", modifier = Modifier.fillMaxSize())
    }
}
