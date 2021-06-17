package it.unibo.skpf.re

import java.io.Serializable

data class BooleanFeatureSet(
    val name: String,
    val set: Map<String, OriginalValue>
) : Serializable {
    companion object {
        const val serialVersionUID = 5329556256856403770L
    }
}
