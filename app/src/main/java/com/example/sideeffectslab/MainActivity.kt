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
import com.example.sideeffectslab.task1.SplashTaskScreen
import com.example.sideeffectslab.task2.OTHER_ROUTE
import com.example.sideeffectslab.task2.OtherScreen
import com.example.sideeffectslab.task2.SnackbarTaskScreen
import com.example.sideeffectslab.task3.LifecycleTaskScreen
import com.example.sideeffectslab.task4.SideEffectTaskScreen
import com.example.sideeffectslab.task5.ScrollTaskScreen
import com.example.sideeffectslab.task6.ProduceStateTaskScreen

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
    LabScreen(route = "task1", title = "1. Splash screen") { SplashTaskScreen() },
    LabScreen(route = "task2", title = "2. Snackbar button") { nav -> SnackbarTaskScreen(onOpenOtherScreen = { nav.navigate(OTHER_ROUTE) }) },
    LabScreen(route = "task3", title = "3. Lifecycle logger") { LifecycleTaskScreen() },
    LabScreen(route = "task4", title = "4. SideEffect and analytics") { SideEffectTaskScreen() },
    LabScreen(route = "task5", title = "5. Scroll: derivedStateOf and snapshotFlow") { ScrollTaskScreen() },
    LabScreen(route = "task6", title = "6. Bonus: produceState") { ProduceStateTaskScreen() },
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
            composable(OTHER_ROUTE) { OtherScreen() } // task2
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
