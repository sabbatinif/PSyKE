package it.unibo.skpf.re

import java.io.Serializable

data class Feature(
    val name: String,
    val admissibleValues: Map<String, Value>
) : Serializable {
    companion object {
        const val serialVersionUID = 5329556256856403770L
    }
}
