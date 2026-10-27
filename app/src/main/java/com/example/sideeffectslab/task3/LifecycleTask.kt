package com.example.sideeffectslab.task3

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val TAG = "Task3"

/** How many times the observer was added / removed (shown on screen to compare). */
object LifecycleStats {
    var added by mutableIntStateOf(0)
    var removed by mutableIntStateOf(0)
}

/**
 * Task 3 – add and remove a listener with DisposableEffect.
 *
 * lifecycleOwner is the key: if a different owner is passed, onDispose removes the observer
 * from the OLD owner first, then the effect adds a new observer to the NEW owner.
 *
 * Check: delete the onDispose { } block and the build fails with a type mismatch – the
 * lambda of DisposableEffect must return a DisposableEffectResult, and the only way to get
 * one is to end the block with onDispose { }.
 */
@Composable
fun LifecycleLogger(lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current) {
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> Log.d(TAG, "ON_START")
                Lifecycle.Event.ON_STOP -> Log.d(TAG, "ON_STOP")
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        LifecycleStats.added++
        Log.d(TAG, "addObserver (total added = ${LifecycleStats.added})")

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            LifecycleStats.removed++
            Log.d(TAG, "removeObserver (total removed = ${LifecycleStats.removed})")
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            "LifecycleLogger is in the Composition. Press Home and reopen the app " +
                "to see ON_STOP / ON_START in Logcat.",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun LifecycleTaskScreen() {
    var showLogger by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = showLogger, onCheckedChange = { showLogger = it })
            Text("  Show LifecycleLogger")
        }
        Text(
            "addObserver: ${LifecycleStats.added}    removeObserver: ${LifecycleStats.removed}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text("Toggle the switch 5 times: when the logger is hidden both numbers are equal.")
        if (showLogger) {
            LifecycleLogger()
        }
    }
}
