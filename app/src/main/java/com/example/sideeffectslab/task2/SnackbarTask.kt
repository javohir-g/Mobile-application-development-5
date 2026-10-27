package com.example.sideeffectslab.task2

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val TAG = "Task2"

const val OTHER_ROUTE = "other"

/**
 * Task 2 – start a coroutine from a callback with rememberCoroutineScope.
 *
 * Why LaunchedEffect does not work here:
 * LaunchedEffect is a @Composable function, and onClick is an ordinary (non-composable)
 * lambda, so `LaunchedEffect(Unit) { snackbarHostState.showSnackbar("Hi") }` inside onClick
 * does not even compile. LaunchedEffect means "run while this composable is in the
 * Composition"; a tap is an event, not a composition, so we need a scope we can launch
 * into on demand.
 *
 * Why it is safe to leave the screen: the scope from rememberCoroutineScope is bound to this
 * composable's place in the Composition. When we navigate away, the screen leaves the
 * Composition, the scope is cancelled and the suspended showSnackbar() call is cancelled with
 * it (Logcat: "cancelled"). No crash, and no snackbar is left behind.
 *
 * Observation – second tap before the first snackbar finishes: SnackbarHostState shows one
 * snackbar at a time (showSnackbar waits on an internal mutex), so the second message is
 * QUEUED and appears only after the first one is dismissed or times out.
 */
@Composable
fun SnackbarTaskScreen(onOpenOtherScreen: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var taps by remember { mutableIntStateOf(0) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // the outer Scaffold in MainActivity already applies the system-bar insets
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = {
                taps++
                val tap = taps
                scope.launch {
                    Log.d(TAG, "tap #$tap: showSnackbar() called")
                    try {
                        // suspend: returns only when the snackbar is dismissed / times out
                        val result = snackbarHostState.showSnackbar(
                            message = "Hello from tap #$tap",
                            actionLabel = "OK",
                        )
                        Log.d(TAG, "tap #$tap: snackbar finished, result=$result")
                    } catch (e: CancellationException) {
                        Log.d(TAG, "tap #$tap: cancelled – the screen left the Composition")
                        throw e
                    }
                }
            }) { Text("Show snackbar") }

            OutlinedButton(onClick = onOpenOtherScreen) { Text("Go to another screen") }

            Text("Taps: $taps", style = MaterialTheme.typography.titleMedium)
            Text(
                "Try: tap twice quickly – the second snackbar waits in a queue. " +
                    "Tap, then open another screen while the snackbar is visible and come back.",
            )
        }
    }
}

@Composable
fun OtherScreen() {
    Column(Modifier.padding(16.dp)) {
        Text("Another screen", style = MaterialTheme.typography.titleLarge)
        Text("Press Back to return to Task 2. The previous screen left the Composition.")
    }
}
