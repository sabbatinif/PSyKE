package smile.data

import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY

/**
 * A range to be used for continuous feature discretisation.
 * @param mean is the initial value of the range.
 * @param std controls the size of the range expansion.
 */
data class Range(
    val mean: Double,
    val std: Double
) {
    /**
     * @property lower is the range lowerbound.
     */
    var lower: Double = this.mean

    /**
     * @property upper is the range upperbound.
     */
    var upper: Double = this.mean

    /**
     * Makes the range right-bounded infinite.
     */
    fun leftInfinite() {
        this.lower = NEGATIVE_INFINITY
    }

    /**
     * Makes the range left-bounded infinite.
     */
    fun rightInfinite() {
        this.upper = POSITIVE_INFINITY
    }

    /**
     * Expands the range towards right.
     */
    fun expandRight() {
        this.upper += this.std
    }

    /**
     * Expands the range towards left.
     */
    fun expandLeft() {
        this.lower -= this.std
    }
}
