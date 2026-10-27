package com.example.sideeffectslab.task4

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sideeffectslab.util.Counter

private const val TAG = "Task4"

/** A simple analytics library: plain Kotlin, NOT managed by Compose. */
object Analytics {
    var userType: String = "unknown"
}

/**
 * Task 4 – publish Compose state to non-Compose code with SideEffect.
 *
 * Why SideEffect runs even when userType did not change: SideEffect has no keys. It is
 * scheduled after EVERY successful (committed) recomposition of the scope it is in. Tapping
 * "Recompose only" changes another state read in the same scope, the scope recomposes and the
 * SideEffect runs again with the same userType (Logcat counter goes up).
 *
 * Attempt model: a recomposition is only an attempt and can be discarded before it is
 * committed; a direct call in the body would already have pushed the value of the discarded
 * attempt to Analytics, while SideEffect waits and runs only when the attempt commits.
 */
@Composable
fun SideEffectTaskScreen() {
    var userType by remember { mutableStateOf("Guest") }
    var unrelatedTaps by remember { mutableIntStateOf(0) }
    val sideEffectRuns = remember { Counter() }

    SideEffect {
        sideEffectRuns.value++
        Analytics.userType = userType
        Log.d(TAG, "SideEffect run #${sideEffectRuns.value}: Analytics.userType = $userType")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("userType state: $userType", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Guest", "Free", "Premium").forEach { type ->
                Button(onClick = { userType = type }) { Text(type) }
            }
        }
        // Reads a different state in the SAME scope -> recomposition without a userType change.
        Text("Unrelated taps: $unrelatedTaps")
        OutlinedButton(onClick = { unrelatedTaps++ }) { Text("Recompose only (userType unchanged)") }
        // Setting the same value is conflated: no recomposition -> no SideEffect run.
        OutlinedButton(onClick = { userType = userType }) { Text("Set the same userType again") }
        Text("SideEffect run counter is printed to Logcat (tag: $TAG).")
    }
}
