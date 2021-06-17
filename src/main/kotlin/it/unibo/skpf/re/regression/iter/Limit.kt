package it.unibo.skpf.re.regression.iter

import it.unibo.skpf.re.regression.HyperCube

internal data class Limit(
    val cube: HyperCube,
    val feature: String,
    val direction: Char
)
