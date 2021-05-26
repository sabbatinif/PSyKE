package it.unibo.skpf.re

sealed class OriginalValue {
    data class Interval(
        val lower: Double,
        val upper: Double
    ) : OriginalValue()

    data class Value(
        val value: Any
    ) : OriginalValue()
}

