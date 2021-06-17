package it.unibo.skpf.re.schema

import java.io.Serializable
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY

/**
 * Class representing constraints on feature values.
 */
sealed class Value : Serializable {
    /**
     * Class representing a continuous interval.
     * @param lower is the lower bound.
     * @param upper is the upper bound.
     */
    sealed class Interval(val lower: Double, val upper: Double) : Value() {
        /**
         * Class representing a right-bounded infinite interval.
         * @param value is the bound.
         */
        data class LessThan(val value: Double) : Interval(NEGATIVE_INFINITY, value)

        /**
         * Class representing a left-bounded infinite interval.
         * @param value is the bound.
         */
        data class GreaterThan(val value: Double) : Interval(value, POSITIVE_INFINITY)

        /**
         * Class representing a finite interval.
         * @param lowerBound is the lower bound.
         * @param upperBound is the upper bound.
         */
        data class Between(val lowerBound: Double, val upperBound: Double) : Interval(lowerBound, upperBound)
    }

    /**
     * Class representing a punctual value.
     * @param value is the exact value.
     */
    data class Constant(val value: Any) : Value()
}
