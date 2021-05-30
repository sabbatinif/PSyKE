package it.unibo.skpf.re

import java.io.Serializable

sealed class OriginalValue: Serializable {
    data class Interval(
        val lower: Double,
        val upper: Double
    ) : OriginalValue()

    data class Value(
        val value: Any
    ) : OriginalValue()
}

