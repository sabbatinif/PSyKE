package it.unibo.skpf.re

import java.io.Serializable
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY

sealed class OriginalValue: Serializable {
    sealed class Interval(
        val lower: Double,
        val upper: Double
    ) : OriginalValue() {

        data class LessThan(
            val value: Double
        ) : Interval(NEGATIVE_INFINITY, value)

        data class GreaterThan(
            val value: Double
        ) : Interval(value, POSITIVE_INFINITY)

        data class Between(
            val lowerbound: Double,
            val upperbound: Double
        ) : Interval(lowerbound, upperbound)
    }

    data class Value(
        val value: Any
    ) : OriginalValue()
}

