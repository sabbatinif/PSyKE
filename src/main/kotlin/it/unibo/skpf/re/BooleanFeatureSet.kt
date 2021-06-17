package it.unibo.skpf.re

import java.io.Serializable

/**
 * Data structure for keep trace of the DataFrame discretisation.
 * @param name is the name of the feature to discretise.
 * @param set is a Map where keys represent the name of the
 *            discretised feature and the value contains the
 *            effective mapping with the original feature values.
 */
data class BooleanFeatureSet(
    val name: String,
    val set: Map<String, OriginalValue>
) : Serializable
