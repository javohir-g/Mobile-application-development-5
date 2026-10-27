package com.example.sideeffectslab.task1

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val TAG = "Task1"
private const val SplashWaitTimeMillis = 2_000L
private const val VersionTickMillis = 500L
private const val MaxVersion = 10

/**
 * Task 1 – the CORRECT splash screen.
 *
 * LaunchedEffect(true): the wait must run exactly once while the splash is in the
 * Composition, so the key is a constant on purpose.
 * rememberUpdatedState: the parent may pass a new onTimeout on every recomposition;
 * currentOnTimeout always points to the newest one, without restarting the effect.
 */
@Composable
fun LandingScreen(onTimeout: () -> Unit, modifier: Modifier = Modifier) {
    val currentOnTimeout by rememberUpdatedState(onTimeout)
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        Log.d(TAG, "updatedState: wait started")
        delay(SplashWaitTimeMillis)
        currentOnTimeout()
        finished = true
    }
    SplashCard(label = "rememberUpdatedState", finished = finished, modifier = modifier)
}

/**
 * Task 1 – the BROKEN variant: the coroutine closes over the onTimeout parameter of the
 * FIRST composition. Later recompositions pass new lambdas, but the running coroutine
 * never sees them, so after 2 s it calls the stale one (prints v0).
 */
@Composable
fun LandingScreenCaptured(onTimeout: () -> Unit, modifier: Modifier = Modifier) {
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        Log.d(TAG, "captured: wait started")
        delay(SplashWaitTimeMillis)
        onTimeout() // stale: captured when the effect was launched
        finished = true
    }
    SplashCard(label = "captured lambda", finished = finished, modifier = modifier)
}

/**
 * Task 1 check – LaunchedEffect(onTimeout).
 *
 * Why the wait restarts: the parent creates a NEW lambda every 500 ms, so the key of the
 * effect changes every 500 ms. A key change cancels the running coroutine (the delay is
 * thrown away) and launches a new one, so the 2 s wait starts over again and again
 * ("keyed: wait (re)started" in Logcat). It only fires 2 s after the counter stops.
 * A splash timer must not depend on a callback that can change -> use a constant key
 * and rememberUpdatedState instead.
 */
@Composable
fun LandingScreenKeyed(onTimeout: () -> Unit, modifier: Modifier = Modifier) {
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(onTimeout) {
        Log.d(TAG, "keyed: wait (re)started")
        delay(SplashWaitTimeMillis)
        onTimeout()
        finished = true
    }
    SplashCard(label = "LaunchedEffect(onTimeout)", finished = finished, modifier = modifier)
}

/** The parent: a version counter that increases every 500 ms and creates new lambdas. */
@Composable
fun SplashTaskScreen() {
    var runId by remember { mutableIntStateOf(0) }
    var version by remember { mutableIntStateOf(0) }
    var showKeyed by remember { mutableStateOf(false) }
    val events = remember { mutableStateListOf<String>() }

    LaunchedEffect(runId) {
        while (version < MaxVersion) {
            delay(VersionTickMillis)
            version++
        }
    }

    // v is a plain Int copied from the state, so every lambda below captures the value of the
    // composition it was created in -> a NEW onTimeout instance for every version.
    val v = version
    val onTimeoutCaptured: () -> Unit = { record(events, "captured -> onTimeout v$v") }
    val onTimeoutUpdated: () -> Unit = { record(events, "updatedState -> onTimeout v$v") }
    val onTimeoutKeyed: () -> Unit = { record(events, "keyed -> onTimeout v$v") }

    val restart: () -> Unit = {
        version = 0
        events.clear()
        runId++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Parent version: v$version", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = showKeyed, onCheckedChange = { showKeyed = it; restart() })
            Text("Also run LaunchedEffect(onTimeout)")
        }
        Button(onClick = restart) { Text("Restart demo") }

        // key(runId): "Restart demo" throws the splash screens away and creates new ones.
        key(runId) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LandingScreenCaptured(onTimeout = onTimeoutCaptured, modifier = Modifier.weight(1f))
                LandingScreen(onTimeout = onTimeoutUpdated, modifier = Modifier.weight(1f))
                if (showKeyed) {
                    LandingScreenKeyed(onTimeout = onTimeoutKeyed, modifier = Modifier.weight(1f))
                }
            }
        }

        Text("Events (Logcat tag: $TAG)", style = MaterialTheme.typography.titleSmall)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(events) { Text(it) }
        }
    }
}

@Composable
private fun SplashCard(label: String, finished: Boolean, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(if (finished) "onTimeout() called" else "waiting 2 s…")
        }
    }
}

private fun record(events: MutableList<String>, message: String) {
    Log.d(TAG, message)
    events.add(message)
}
