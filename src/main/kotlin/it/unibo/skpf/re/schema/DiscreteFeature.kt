package it.unibo.skpf.re.schema

import java.io.Serializable

/**
 * Data structure for keeping trace of the DataFrame discretisation.
 * @param name is the name of the feature before discretisation.
 * @param admissibleValues is a Map where keys represent the name of the
 *                         discretised feature and the value contains the
 *                         effective mapping with the original feature values.
 */
data class DiscreteFeature(
    val name: String,
    val admissibleValues: Map<String, Value>
) : Serializable {

    constructor(name: String, vararg admissibleValues: Pair<String, Value>) : this(name, mapOf(*admissibleValues))
}
