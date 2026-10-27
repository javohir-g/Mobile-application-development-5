# SideEffectsLab

Practice class work for **Lecture 13 – Side-effects in Jetpack Compose**
(LaunchedEffect · rememberCoroutineScope · rememberUpdatedState · DisposableEffect ·
SideEffect · produceState · derivedStateOf · snapshotFlow).

One app, one screen per task. The home screen lists the six tasks. Every task was added in
its own commit (see `git log`).

## How to run

1. Open the `SideEffectsLab` folder in Android Studio and let Gradle sync
   (AGP 8.7, Kotlin 2.0, Compose BOM 2024.12, compileSdk 35, minSdk 24).
2. Run the `app` configuration on an emulator or a phone.
3. In Logcat filter by tag: `tag:Task1`, `tag:Task3`, `tag:Task4`, `tag:Task5`, `tag:Task6`.

## Which effect API in which task, and why

| Task | Screen | API | Why this API |
|---|---|---|---|
| 1 | Splash screen | `LaunchedEffect(true)` + `rememberUpdatedState` | The 2 s wait is suspend work that must start once, when the screen appears, so it is a `LaunchedEffect` with a constant key. The parent passes a new `onTimeout` every 500 ms; `rememberUpdatedState` lets the running effect call the newest lambda without restarting the wait. The screen also shows the captured-lambda bug (prints `v0`) and the `LaunchedEffect(onTimeout)` variant, where the wait restarts on every new lambda. |
| 2 | Snackbar button | `rememberCoroutineScope` | The work starts because the user tapped, not because the composable appeared. `LaunchedEffect` is `@Composable` and cannot be called from `onClick`. The scope is cancelled when the screen leaves the Composition, so the suspended `showSnackbar` is cancelled too. |
| 3 | Lifecycle logger | `DisposableEffect(lifecycleOwner)` | Registering a listener needs a matching cleanup. `onDispose` removes the observer when the composable leaves or when `lifecycleOwner` (the key) changes, so add and remove lines are always balanced. |
| 4 | Analytics | `SideEffect` | `Analytics` is a plain object that Compose does not manage. `SideEffect` pushes the current `userType` only after a recomposition has been committed, never from a discarded attempt. It has no keys, so it runs after every recomposition. |
| 5 | Scroll | `derivedStateOf` + `snapshotFlow` in `LaunchedEffect` | The button needs a Boolean (`index > 0`) that changes far less often than the index, which is what `derivedStateOf` is for. A direct-read variant is shown next to it for comparison. The analytics event needs a Flow from Compose state (`snapshotFlow`), collected in a `LaunchedEffect` that is tied to the list. |
| 6 | Bonus | `produceState` | Turns the suspend `load(url)` into `State<ImageResult>`. The producer starts on enter, restarts when the URL changes (Loading again) and is cancelled on leave (the `finally` line). |

## Logcat excerpts

> Paste real Logcat output from the device here; these are the lines to look for.

### Task 1 – Splash screen (`tag:Task1`)
Expected: `captured -> onTimeout v0` and `updatedState -> onTimeout v3` (or `v4`), meaning the latest version.
With the checkbox on, `keyed: wait (re)started` appears every 500 ms, and `keyed -> onTimeout v10` appears only about 2 s after the counter stops.

```
(paste here)
```

### Task 3 – Lifecycle logger (`tag:Task3`)
Expected: after toggling the switch 5 times, the `addObserver` and `removeObserver` counts are equal. `ON_STOP` / `ON_START` appear after pressing Home and coming back.

```
(paste here)
```

### Task 4 – SideEffect (`tag:Task4`)
Expected: `SideEffect run #N` grows on every recomposition, including the "Recompose only" button where userType did not change. "Set the same userType again" adds no line.

```
(paste here)
```

### Task 5 – Scroll (`tag:Task5`)
Expected: many `direct read variant: composition #N` lines, few `derivedStateOf variant: composition #N` lines, and one `EVENT scrolled_past_first_item` per crossing of item 0.

```
(paste here)
```

## Observations

- **Task 1:** with `LaunchedEffect(onTimeout)` the key changes every 500 ms, so the coroutine is cancelled and relaunched and the 2 s wait starts over each time.
- **Task 2:** a second tap before the first snackbar is gone does not replace it. `SnackbarHostState` queues the second message and shows it after the first one is dismissed or times out. Leaving the screen cancels the pending `showSnackbar` (Logcat `Task2`: `cancelled`).
- **Task 3:** deleting `onDispose { }` gives a compile-time type mismatch, because the `DisposableEffect` block must return a `DisposableEffectResult`.
- **Task 4:** `SideEffect` has no keys and runs after *every* committed recomposition of its scope.
- **Task 6:** writing an equal value to the produced state does not cause a recomposition, because `MutableState` uses structural equality. The three "set the same value again" lines are not followed by composition lines.

Part A answers: [PART_A_ANSWERS.md](PART_A_ANSWERS.md)
