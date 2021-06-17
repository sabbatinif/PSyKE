package smile.data

/**
 * Description of a DataFrame feature.
 * @property mean is the feature mean value.
 * @property stdDev is the feature standard deviation.
 * @property min is the feature min value.
 * @property max is the feature max value.
 */
data class Description(
    val mean: Double,
    val stdDev: Double,
    val min: Double,
    val max: Double
)
