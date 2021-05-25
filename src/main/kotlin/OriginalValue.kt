interface OriginalValue

data class Interval(
    val lower: Double,
    val upper: Double
) : OriginalValue

data class Value(
    val value: Any
) : OriginalValue

data class BooleanFeatureSet(
    val name: String,
    val set: Map<String, OriginalValue>
)