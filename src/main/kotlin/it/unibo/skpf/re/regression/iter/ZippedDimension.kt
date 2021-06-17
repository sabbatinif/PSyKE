package it.unibo.skpf.re.regression.iter

internal data class ZippedDimension(
    val dimension: String,
    val thisCube: Pair<Double, Double>,
    val otherCube: Pair<Double, Double>
)
