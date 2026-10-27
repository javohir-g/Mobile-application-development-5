# Part A – Theory questions and code reading (Side-effects, Lecture 13)

## Question 1 – Side-effects in the composable body

A **side-effect** is any change to the state of the app that is visible outside the scope of
a composable function, for example a network request, a database write, an analytics log or
registering a listener.

The composable body is a dangerous place for side-effects because Compose decides when and
how often it runs:
- it can run **very often**, even on every frame while an animation is running;
- it can be **skipped**, or run in a different order, or in parallel;
- a recomposition is **optimistic**: if state changes again in the middle of it, Compose
  **cancels the attempt, throws it away and starts again**.

So `viewModel.loadProfile()` and `analytics.log("opened")` in the body run once per
recomposition (many requests during the animation), and they can even run for an attempt
that is never shown.

**Effect (Key Term), in my words:** an effect is a composable that draws no UI. Its only job
is to run side-effect code at a controlled moment, after the composition has been
committed, and to cancel, clean up or restart that code together with the composable's
lifecycle and keys.

**Why loading belongs in the ViewModel (unidirectional data flow):** state flows down
(ViewModel → UI) and events flow up (UI → ViewModel). The ViewModel survives recompositions
and configuration changes, so it loads the profile once (in `init` or when it receives an
event) and exposes the result as state. The composable only shows that state and stays
free of side-effects (idempotent). Analytics that must fire once when the screen appears
goes into `LaunchedEffect(Unit)`.

## Question 2 – Which API?

| I want to | API |
|---|---|
| call a suspend function while the composable is in the Composition | `LaunchedEffect` |
| launch a coroutine from a callback such as onClick | `rememberCoroutineScope` |
| read the latest value inside an effect without restarting it | `rememberUpdatedState` |
| register a listener and remove it afterwards | `DisposableEffect` |
| publish Compose state to code Compose does not manage | `SideEffect` |
| turn Flow, LiveData or RxJava into Compose State | `produceState` (`collectAsState` / `observeAsState` / `subscribeAsState` are built on it) |
| recompose less often than the input changes | `derivedStateOf` |
| turn Compose State into a Flow | `snapshotFlow` |

## Question 3 – PulseEffect

1. **Start:** when the composable enters the Composition. After the first composition is
   committed, `LaunchedEffect` launches its block as a coroutine in the composition's
   coroutine context.
2. **pulseRateMs 3000 → 1000:** the key changed, so on the next recomposition the running
   coroutine is **cancelled** (the current `delay`/`animateTo` throws
   `CancellationException`) and a **new** coroutine is launched with `pulseRateMs = 1000`.
   The loop starts over with the new rate.
3. **Leaving the Composition:** the coroutine is cancelled, the suspended `delay` or
   `animateTo` throws `CancellationException` and the `while` loop ends. `isActive` is
   `true` only while the coroutine's `Job` is active (not cancelled and not completed), so
   it makes the loop cooperate with cancellation.
   **MiniComposition model:** when the key changes or the composable leaves, the model
   prints `effect(100) cancelled` *before* (or instead of) starting a new effect. An effect
   lives exactly as long as its key and its place in the composition: old one cancelled,
   new one launched on a key change, only cancelled on leave.

## Question 4 – LaunchedEffect(true)

A constant key never changes, so the effect never restarts while the composable is shown.
Like `while(true)`, it silently ignores everything that changes. Here, if `userId` changes,
the hello is never sent for the new user. The effect keeps working with the stale value
it captured at the start.

A constant key is right only when the effect must run **exactly once for the whole time
the composable is in the Composition** and must not restart: the splash screen that waits
2 s and then calls `onTimeout` (together with `rememberUpdatedState` for the callback).

```kotlin
LaunchedEffect(userId) {
    api.sendHello(userId)   // restarts (and sends again) whenever userId changes
}
```

## Question 5 – LaunchedEffect in onClick

`LaunchedEffect` is a `@Composable` function, so it can only be called during
composition. `onClick` is an ordinary lambda that runs later, outside composition, in
response to an event, so the code does not compile.

`rememberCoroutineScope()` returns a `CoroutineScope` bound to the point in the
Composition where it was called. The same scope is kept across recompositions. When that
call site **leaves the Composition the scope is cancelled**, together with every
coroutine launched in it.

```kotlin
val snackbarHostState = remember { SnackbarHostState() }
val scope = rememberCoroutineScope()
Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
    Button(onClick = {
        scope.launch { snackbarHostState.showSnackbar("Hi") }
    }) { Text("Show") }
}
```

`showSnackbar` is a **suspend** function: it suspends until the snackbar is dismissed or
times out. It therefore needs a coroutine. Because that coroutine lives in the
composition-bound scope, leaving the screen cancels it, so the snackbar disappears and
nothing leaks.

- Work that starts because the composable **appeared** → `LaunchedEffect`.
- Work that starts because the **user did something** → `rememberCoroutineScope`.

## Question 6 – LandingScreen and rememberUpdatedState

`LaunchedEffect(true)` launches its block once. The block closes over the `onTimeout`
parameter of the **first** composition. When the parent recomposes and passes a new
lambda, the coroutine that is already running still holds the old reference, so after
the delay it calls a **stale** lambda.

`rememberUpdatedState(onTimeout)` is a remembered `State` whose value is replaced with the
newest `onTimeout` on every recomposition. The effect reads `currentOnTimeout` only when
the time is up, so it calls the latest lambda, and the effect itself never restarts.

Making `onTimeout` the key is wrong: every new lambda from the parent cancels the coroutine
and launches a new one, so the 2 s wait starts over each time. The splash finishes late,
or never if the parent keeps recomposing. The wait has nothing to do with the identity of
the callback.

**Model output:** `captured -> onTimeout v1` vs `updatedState -> onTimeout v3`. Both
effects ran once and waited the same time, but the captured one called the version from
when it started (v1), while the `rememberUpdatedState` one called the version that was
current when it fired (v3). The output shows that the effect did not restart and still
called the newest callback.

## Question 7 – DisposableEffect without onDispose

`addObserver` makes the lifecycle hold a strong reference to the observer, and the
observer captures the composable's lambdas and state (and through them maybe the
Activity). Without `removeObserver` the lifecycle keeps the observer alive after the
composable is gone, and keeps calling it for UI that no longer exists. Every time the
screen comes back, another observer is added. This is a **leak**, with duplicated events.

**onDispose rule:** the block of `DisposableEffect` must **end with `onDispose { }`** as
its last statement. That call returns the `DisposableEffectResult` the block must return.
Without it the code does not compile.

An **empty** `onDispose` means there is nothing to clean up, so `DisposableEffect` is the
wrong tool. Use `SideEffect` (publish state after each commit) or `LaunchedEffect`
(one-shot or suspend work) instead.

```kotlin
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event -> /* ... */ }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
    }
}
```

## Question 8 – MiniDisposable model

```
A.addObserver(observer)
A.removeObserver(observer)
B.addObserver(observer)
B.removeObserver(observer)
```

- The second `screen("A")` prints nothing: the key (`"A"`) is the same, so the effect is
  kept as it is and is neither disposed nor run again.
- When `B` arrives the key changed: **the old effect is cleaned up first**
  (`A.removeObserver`), **then** the new effect runs (`B.addObserver`).
- `comp.leave()` disposes the current effect: `B.removeObserver`.

## Question 9 – SideEffect

`SideEffect` runs **after every successful recomposition**, once the composition has been
committed (applied). It has no keys and runs again after every recomposition of its scope.

A direct call in the body runs **during** composition, and a composition attempt can be
thrown away before it is committed. The library would already have received a value from
a UI that never appeared on screen.

Attempt model:
- **direct call, commits = false:** analytics **is called**, which is wrong because the
  attempt was discarded but the library was already changed;
- **SideEffect, commits = false:** analytics is **not** called, because the queued effect is
  dropped together with the attempt;
- **SideEffect, commits = true:** analytics is called **once**, after the commit.

**Keys:** none. `SideEffect` takes no keys.

## Question 10 – produceState and loadNetworkImage

`produceState` starts a coroutine, scoped to the Composition, that pushes values into a
returned `State<T>`. It starts with `initialValue`, and the producer sets `value`.

- The producer **starts** when `produceState` enters the Composition.
- It is **cancelled** when it leaves the Composition.
- It is **cancelled and restarted** when one of its keys changes (e.g. `url`).

The returned State is a `MutableState` with **structural equality**: setting a value
`equals` to the current one is not a change, so it schedules **no recomposition**. Several
fast writes before the next frame also collapse into the latest one. In the lecture's
model, setting the same `Success` value twice increases the recomposition count only once.

**Lowercase name:** by the Compose naming rule, a composable that **emits UI** returns
`Unit` and is named like a noun in PascalCase (`NetworkImage`). A composable that **returns
a value** is named like a normal Kotlin function in camelCase (`remember`, `produceState`,
`loadNetworkImage`).

## Question 11 – myProduceState

```kotlin
@Composable
fun <T> myProduceState(
    initialValue: T,
    key: Any?,
    producer: suspend MutableState<T>.() -> Unit
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffect(key) {
        result.producer()
    }
    return result
}
```

It behaves like `LaunchedEffect` because it **is** a `LaunchedEffect` around a remembered
state. The producer starts when the composable enters the Composition and is cancelled
when it leaves. When `key` changes, the running producer is cancelled and a new one is
launched. The state object is the same, so it keeps the last value until the new producer
writes a new one.

## Question 12 – derivedStateOf for the scroll button

Reading `listState.firstVisibleItemIndex` directly in composition subscribes the scope to
that state. Every index change (1 → 2 → 3 …) invalidates the scope and recomposes the
button, even though `index > 0` is still `true`.

`derivedStateOf { listState.firstVisibleItemIndex > 0 }` recalculates whenever the index
changes, but **notifies its readers only when the result changes**. The button recomposes
only when the Boolean flips.

It is wrapped in `remember` so the same derived-state object, with its cached value and
dependencies, survives recompositions. Without `remember`, every recomposition would
create a new `derivedStateOf` and the caching would be lost.

Indexes `[0, 0, 1, 2, 3, 4, 5, 3, 1, 0]`:
- **Direct read:** 1 initial composition + 1 for every real change. `0 → 0` is not a
  change (same value), then `0→1, 1→2, 2→3, 3→4, 4→5, 5→3, 3→1, 1→0` = 8 changes.
  **1 + 8 = 9.**
- **derivedStateOf:** values of `index > 0` are `F, F, T, T, T, T, T, T, T, F`. It
  flips only twice (`F→T` at index 1, `T→F` at the last 0). **1 + 2 = 3.**

## Question 13 – derivedStateOf as pure overhead

`"$firstName $lastName"` changes **every time** one of the inputs changes. The result
changes exactly as often as the inputs, so `derivedStateOf` saves no recomposition and only
adds the cost of tracking dependencies and an extra state object.

```kotlin
var firstName by remember { mutableStateOf("") }
var lastName by remember { mutableStateOf("") }
val fullName = "$firstName $lastName"
```

**Criterion:** use `derivedStateOf` when the inputs change **more often** than the result you
actually need (scroll index vs. "is it > 0"). If the result changes as often as the inputs,
compute it directly.

**Caution, in my words:** `derivedStateOf` is not free. Don't use it just because one state
is calculated from another. Use it only when it filters out updates the UI does not need.

## Question 14 – Voice-message waveform

**Use `derivedStateOf`** when the waveform depends on a state that changes much more often
than the waveform needs to change. Example: the playback position changes every frame, but
the bar that is highlighted changes only now and then.

**Do not use it** when:
1. **The amplitudes are precomputed:** they are a ready list (computed once with
   `remember(file)`), and nothing changes frequently, so there is nothing to filter.
2. **The waveform is static:** there is no changing state at all. A constant or a
   `remember` is enough, and `derivedStateOf` would only be overhead.
3. **The waveform is updated manually with `LaunchedEffect` or `animate*`:** you already
   decide exactly when the state changes, and every update is a real change you want to
   draw. The result changes as often as the input, so `derivedStateOf` gives nothing.

## Question 15 – snapshotFlow analytics

`snapshotFlow { … }` turns Compose State reads into a Flow. It runs the block, remembers
which State objects were read, and emits a new value whenever one of them changes. Values
that are equal to the previous emission are skipped. It is a **cold** Flow: nothing
happens until someone collects it, every collector runs its own copy, and it stops when
the collecting coroutine is cancelled.

- `map { index -> index > 0 }` turns the index into "past the first item?".
- `distinctUntilChanged()` lets a value through only when it differs from the previous one,
  so only the flips pass.
- `filter { it == true }` keeps only the flips to `true`.

`collect` is a suspend function, so it needs a coroutine. `LaunchedEffect(listState)` ties
that coroutine to the composable: it starts collecting on enter, stops on leave, and
restarts if `listState` changes.

Stream `0, 0, 1, 2, 5, 3, 0, 0, 1, 4`:
1. snapshotFlow drops the repeated equal values: `0, 1, 2, 5, 3, 0, 1, 4`.
2. `map`: `F, T, T, T, T, F, T, T`.
3. `distinctUntilChanged`: `F, T, F, T`.
4. `filter`: `T, T`, so **2 events** (the first time it reaches 1 and the second time).

## Question 16 – Choosing keys (tick 1 → 4, owner A → B at tick 3)

| Keys | Restarts (prediction) | Why |
|---|---|---|
| `keys = []` | 0 restarts (runs once, at tick 1) | No keys, so it never restarts. After tick 3 it still works with owner **A**: **wrong** (stale value). |
| `keys = [owner]` | 1 restart (at tick 3) | Restarts exactly when the data it uses changes: **right**. |
| `keys = [owner, tick]` | 3 restarts (at ticks 2, 3, 4) | Restarts on every tick although the owner changed only once. The values are correct, but it cancels and relaunches for nothing: **inefficient**. If the effect needs the latest `tick`, use `rememberUpdatedState` instead. |

## Question 17 – Keys in HomeScreen

- **Must be a key:** `lifecycleOwner`. The effect adds the observer to it, so when it
  changes the observer must be removed from the old owner and added to the new one.
- **Must not be keys:** `onStart` / `onStop` (or `currentOnStart` / `currentOnStop`).
  `currentOnStart` and `currentOnStop` are `State` objects from `rememberUpdatedState`. They
  are the same objects on every recomposition, and the observer reads their latest value
  when the event happens. Making the callbacks keys would remove and re-add the observer
  every time the parent passes a new lambda.
- **If `lifecycleOwner` is not a key and changes:** the observer stays registered on the
  **old** owner. The screen gets no events from the new owner, and the old owner keeps the
  observer alive (a leak) until the composable leaves.
- **Key Point rule:** every value used inside the effect block that can change must be
  passed as a key of the effect. If a change of that value must **not** restart the effect,
  wrap it in `rememberUpdatedState` instead.

## Question 18 – ChatScreen code review

1. **`viewModel.markOpened(userId)` in the body:** a side-effect in composition. It runs on
   every recomposition and even in discarded attempts. Fix: `LaunchedEffect(userId)`.
2. **`LaunchedEffect(true) { delay(5_000); onClose() }`:** a constant key is fine for a
   one-shot timer, but it captures the `onClose` of the first composition (stale lambda).
   Fix: `val currentOnClose by rememberUpdatedState(onClose)` and call `currentOnClose()`.
3. **`DisposableEffect(owner)` without `onDispose`:** it does not compile, and the observer
   would never be removed (leak). Fix: end with
   `onDispose { owner.lifecycle.removeObserver(observer) }`.
4. **`LaunchedEffect(Unit) { send() }` inside `onClick`:** a composable called from a
   non-composable callback does not compile. Fix: use the `scope` that already exists:
   `scope.launch { send() }`.

```kotlin
@Composable
fun ChatScreen(userId: String, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    val observer = remember { LifecycleEventObserver { _, _ -> } }
    val currentOnClose by rememberUpdatedState(onClose)

    LaunchedEffect(userId) { viewModel.markOpened(userId) }   // (1)
    LaunchedEffect(true) {                                    // (2)
        delay(5_000)
        currentOnClose()
    }
    DisposableEffect(owner) {                                 // (3)
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Button(onClick = {                                        // (4)
        scope.launch { send() }
    }) { Text("Send") }
}
```

## Question 19 – Summary table

| API | Runs | Cleanup | Restarts on |
|---|---|---|---|
| LaunchedEffect | When it enters the Composition (after the commit), its block runs as a coroutine | Coroutine is cancelled when it leaves or before a restart | Key change |
| rememberCoroutineScope | Nothing by itself; you launch coroutines from callbacks | Scope and all its coroutines are cancelled when it leaves | Never (same scope across recompositions) |
| rememberUpdatedState | Updates its value on every recomposition | Nothing to clean up | Never; it exists so the effect does not restart |
| DisposableEffect | Body runs once when it enters (after the commit) | `onDispose` when it leaves or before a restart | Key change |
| SideEffect | After **every** successful (committed) recomposition | None | No keys; runs after every recomposition |
| produceState | Producer coroutine starts when it enters | Producer cancelled when it leaves or before a restart (`awaitDispose` for extra cleanup) | Key change |
| derivedStateOf | Recalculates when a State it reads changes; notifies only when the result changes | None (dropped with its `remember`) | Inputs (State it reads); recreated if the `remember` keys change |
| snapshotFlow | When collected; re-runs the block when a State it read changes | Stops when the collecting coroutine is cancelled | When the collecting effect restarts (its keys) |

**Usage examples:**
- **LaunchedEffect:** load the chat messages when `chatId` changes.
- **rememberCoroutineScope:** open the navigation drawer or show a snackbar when a button
  is tapped.
- **rememberUpdatedState:** a splash or "auto-logout after inactivity" timer that calls the
  latest callback.
- **DisposableEffect:** register a sensor listener, a `BroadcastReceiver`, or a lifecycle
  observer that pauses the video player in `ON_STOP`.
- **SideEffect:** set the current user ID or user type in Firebase Analytics or Crashlytics.
- **produceState:** load an image or a profile from a repository into `Loading / Success /
  Error`.
- **derivedStateOf:** show the "scroll to top" FAB only when the list is scrolled, or
  enable the "Submit" button only when the form is valid.
- **snapshotFlow:** send an analytics event when the pager page changes, or save the scroll
  position with a debounce.
