package com.example.sideeffectslab.task5

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sideeffectslab.util.Counter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "Task5"

object MyAnalyticsService {
    private var sent = 0

    fun sendScrolledPastFirstItemEvent() {
        sent++
        Log.d(TAG, "EVENT scrolled_past_first_item (#$sent)")
    }
}

/**
 * Task 5 – derivedStateOf vs direct read, plus snapshotFlow analytics.
 * Both "scroll to top" variants observe the SAME list, so one scroll drives both counters.
 */
@Composable
fun ScrollTaskScreen() {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrollToTop: () -> Unit = { scope.launch { listState.animateScrollToItem(0) } }

    // Compose State -> Flow. snapshotFlow emits when firstVisibleItemIndex changes;
    // map turns it into "past the first item?", distinctUntilChanged keeps only the flips,
    // filter keeps only false -> true. Result: exactly one event per threshold crossing.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index -> index > 0 }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                MyAnalyticsService.sendScrolledPastFirstItemEvent()
            }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DerivedScrollToTop(listState, scrollToTop, Modifier.weight(1f))
            DirectScrollToTop(listState, scrollToTop, Modifier.weight(1f))
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            items(100) { index ->
                Text(
                    "Item #$index",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
                HorizontalDivider()
            }
        }
    }
}

/** Recomposes only when the Boolean flips (false <-> true), not on every index change. */
@Composable
private fun DerivedScrollToTop(listState: LazyListState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val compositions = remember { Counter() }
    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    SideEffect {
        compositions.value++
        Log.d(TAG, "derivedStateOf variant: composition #${compositions.value}")
    }
    // value read during this composition = runs so far + this one
    VariantCard("derivedStateOf", compositions.value + 1, showButton, onClick, modifier)
}

/** Reads the index directly: recomposes on EVERY firstVisibleItemIndex change. */
@Composable
private fun DirectScrollToTop(listState: LazyListState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val compositions = remember { Counter() }
    val showButton = listState.firstVisibleItemIndex > 0
    SideEffect {
        compositions.value++
        Log.d(TAG, "direct read variant: composition #${compositions.value}")
    }
    VariantCard("direct read", compositions.value + 1, showButton, onClick, modifier)
}

@Composable
private fun VariantCard(
    title: String,
    compositions: Int,
    showButton: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text("compositions: $compositions", style = MaterialTheme.typography.titleMedium)
            if (showButton) {
                Button(onClick = onClick) { Text("Scroll to top") }
            } else {
                Text("(at the top)")
            }
        }
    }
}
