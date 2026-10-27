package com.example.sideeffectslab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                LabApp()
            }
        }
    }
}

/** One screen of the lab = one hands-on task. */
data class LabScreen(
    val route: String,
    val title: String,
    val content: @Composable (NavController) -> Unit,
)

private const val HOME_ROUTE = "home"

val labScreens: List<LabScreen> = listOf(
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val title = labScreens.firstOrNull { it.route == route }?.title ?: "SideEffectsLab"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (route != null && route != HOME_ROUTE) {
                        TextButton(onClick = { navController.popBackStack() }) { Text("Back") }
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HOME_ROUTE,
            modifier = Modifier.padding(padding),
        ) {
            composable(HOME_ROUTE) { HomeScreen(navController) }
            labScreens.forEach { screen ->
                composable(screen.route) { screen.content(navController) }
            }
        }
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Side-effects practice: one screen per task", style = MaterialTheme.typography.titleMedium)
        labScreens.forEach { screen ->
            Button(onClick = { navController.navigate(screen.route) }, modifier = Modifier.fillMaxWidth()) {
                Text(screen.title)
            }
        }
    }
}
