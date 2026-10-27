package com.example.sideeffectslab.util

/**
 * A plain (non-State) counter. Incrementing it inside SideEffect does NOT
 * trigger a new recomposition, so it can safely count recompositions.
 */
class Counter {
    var value: Int = 0
}
