package it.unibo.skpf.re.schema

sealed class Schema(protected open val features: Iterable<Feature>) : Iterable<Feature> by features {
    data class Ordered(override val features: List<Feature>) : Schema(features), List<Feature> by features {
        override fun iterator(): Iterator<Feature> {
            return features.iterator()
        }
    }
    data class Unordered(override val features: Set<Feature>) : Schema(features), Set<Feature> by features {
        override fun iterator(): Iterator<Feature> {
            return features.iterator()
        }
    }
}