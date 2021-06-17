package it.unibo.skpf.re

import java.io.Serializable
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY

/**
 * Class representing constraints on feature values.
 */
sealed class OriginalValue : Serializable {
    /**
     * Class representing a continuous interval.
     * @param lower is the lowerbound.
     * @param upper is the upperbound.
     */
    sealed class Interval(
        val lower: Double,
        val upper: Double
    ) : OriginalValue() {
        /**
         * Class representing a right-bounded infinite interval.
         * @param value is the bound.
         */
        data class LessThan(
            val value: Double
        ) : Interval(NEGATIVE_INFINITY, value)

        /**
         * Class representing a left-bounded infinite interval.
         * @param value is the bound.
         */
        data class GreaterThan(
            val value: Double
        ) : Interval(value, POSITIVE_INFINITY)

        /**
         * Class representing a finite interval.
         * @param lowerbound is the lowerbound.
         * @param upperbound is the upperbound.
         */
        data class Between(
            val lowerbound: Double,
            val upperbound: Double
        ) : Interval(lowerbound, upperbound)
    }

    /**
     * Class representing a punctual value.
     * @param value is the exact value.
     */
    data class Value(
        val value: Any
    ) : OriginalValue()
}
