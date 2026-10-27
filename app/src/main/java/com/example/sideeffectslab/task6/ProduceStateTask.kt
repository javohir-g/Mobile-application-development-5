package com.example.sideeffectslab.task6

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sideeffectslab.util.Counter
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay

private const val TAG = "Task6"

/** Result of loading an image (named ImageResult so it does not clash with kotlin.Result). */
sealed interface ImageResult {
    data object Loading : ImageResult
    data class Success(val url: String) : ImageResult
    data class Error(val message: String) : ImageResult
}

/** Fake repository: 500 ms delay, Success only for .png URLs. */
class FakeImageRepository {
    suspend fun load(url: String): ImageResult {
        delay(500)
        return if (url.endsWith(".png")) {
            ImageResult.Success(url)
        } else {
            ImageResult.Error("Not a .png image: $url")
        }
    }
}

/**
 * Task 6 – turn a suspend source into Compose State with produceState.
 *
 * produceState = remember { mutableStateOf(initial) } + LaunchedEffect(keys). The producer
 * starts when this enters the Composition, restarts when url/repository change (Loading
 * again) and is cancelled when it leaves (the finally line in Logcat).
 * The name starts with a lowercase letter because the composable RETURNS a value.
 */
@Composable
fun loadNetworkImage(url: String, repository: FakeImageRepository): State<ImageResult> {
    return produceState<ImageResult>(ImageResult.Loading, url, repository) {
        Log.d(TAG, "producer started for $url")
        try {
            value = ImageResult.Loading
            val result = repository.load(url)
            value = result
            Log.d(TAG, "value = $result")

            // Experiment: set the SAME value again. The State uses structural equality, so
            // these writes are conflated – no "NetworkImage composition" lines follow them.
            repeat(3) { i ->
                delay(1_000)
                value = result
                Log.d(TAG, "set the same value again (${i + 1}/3)")
            }
            awaitCancellation() // keep the producer alive until url changes or we leave
        } finally {
            Log.d(TAG, "finally: producer for $url cancelled")
        }
    }
}

@Composable
fun ProduceStateTaskScreen() {
    val repository = remember { FakeImageRepository() }
    var url by rememberSaveable { mutableStateOf("https://example.com/cat.png") }
    var showImage by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("URL: $url", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { url = "https://example.com/cat.png" }) { Text("cat.png") }
            Button(onClick = { url = "https://example.com/dog.png" }) { Text("dog.png") }
            Button(onClick = { url = "https://example.com/cat.jpg" }) { Text("cat.jpg") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = showImage, onCheckedChange = { showImage = it })
            Text("  Show NetworkImage")
        }
        if (showImage) {
            NetworkImage(url, repository)
        }
    }
}

@Composable
private fun NetworkImage(url: String, repository: FakeImageRepository) {
    val compositions = remember { Counter() }
    // read .value HERE so this scope (and its SideEffect counter) recomposes on changes
    val image = loadNetworkImage(url, repository).value
    SideEffect {
        compositions.value++
        Log.d(TAG, "NetworkImage composition #${compositions.value}: $image")
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (image) {
                ImageResult.Loading -> {
                    CircularProgressIndicator()
                    Text("Loading…")
                }
                is ImageResult.Success -> Text("Success: ${image.url}")
                is ImageResult.Error -> Text("Error: ${image.message}", color = MaterialTheme.colorScheme.error)
            }
            Text("compositions: ${compositions.value + 1}")
        }
    }
}
