package it.unibo.skpf.re

import java.io.Serializable

data class BooleanFeatureSet(
    val name: String,
    val set: Map<String, OriginalValue>
): Serializable