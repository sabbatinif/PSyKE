package it.unibo.skpf.re

import java.io.Serializable
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY

sealed class Value : Serializable {
    sealed class Interval(val lower: Double, val upper: Double) : Value() {
        data class LessThan(val value: Double) : Interval(NEGATIVE_INFINITY, value)

        data class GreaterThan(val value: Double) : Interval(value, POSITIVE_INFINITY)

        data class Between(val lowerBound: Double, val upperBound: Double) : Interval(lowerBound, upperBound)
    }

    data class Constant(val value: Any) : Value()

    companion object {
        const val serialVersionUID = -7371941503712701496L
    }
}
