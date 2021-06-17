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
     * Opens the range lowerbound.
     */
    fun openLower() {
        this.lower = NEGATIVE_INFINITY
    }

    /**
     * Opens the range upperbound.
     */
    fun openUpper() {
        this.upper = POSITIVE_INFINITY
    }
}
