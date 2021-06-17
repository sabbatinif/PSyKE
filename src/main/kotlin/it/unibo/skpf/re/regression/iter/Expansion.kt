package it.unibo.skpf.re.regression.iter

import it.unibo.skpf.re.regression.HyperCube

internal data class Expansion(
    val cube: HyperCube,
    val feature: String,
    val direction: Char,
    val distance: Double
) {
    val values: Pair<Double, Double>
        get() = cube.get(feature)
}
