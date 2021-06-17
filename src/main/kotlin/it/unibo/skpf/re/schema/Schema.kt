package it.unibo.skpf.re.schema

import java.io.Serializable

sealed class Schema(
    protected open val features: Collection<Feature>
) : Collection<Feature> by features, Serializable {

    data class Ordered(
        override val features: List<Feature> = emptyList()
    ) : Schema(features), List<Feature> by features {

        constructor(vararg features: Feature) : this(listOf(*features))

        override fun iterator(): Iterator<Feature> = features.iterator()

        override fun contains(element: Feature): Boolean = features.contains(element)

        override fun containsAll(elements: Collection<Feature>): Boolean = features.containsAll(elements)

        override fun isEmpty(): Boolean = features.isEmpty()

        override val size: Int
            get() = features.size
    }

    data class Unordered(
        override val features: Set<Feature> = emptySet()
    ) : Schema(features), Set<Feature> by features {

        constructor(vararg features: Feature) : this(setOf(*features))

        override fun iterator(): Iterator<Feature> = features.iterator()

        override fun contains(element: Feature): Boolean = features.contains(element)

        override fun containsAll(elements: Collection<Feature>): Boolean = features.containsAll(elements)

        override fun isEmpty(): Boolean = features.isEmpty()

        override val size: Int
            get() = features.size
    }

    object Empty : Schema(emptyList())

    companion object {
        const val serialVersionUID = 5329536256856403760L
    }
}
