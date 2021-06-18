package it.unibo.skpf.re.schema

import java.io.Serializable

sealed class Schema(
    protected open val features: Collection<DiscreteFeature>
) : Collection<DiscreteFeature> by features, Serializable {

    data class Ordered(
        override val features: List<DiscreteFeature> = emptyList()
    ) : Schema(features), List<DiscreteFeature> by features {

        constructor(vararg features: DiscreteFeature) : this(listOf(*features))

        override fun iterator(): Iterator<DiscreteFeature> = features.iterator()

        override fun contains(element: DiscreteFeature): Boolean = features.contains(element)

        override fun containsAll(elements: Collection<DiscreteFeature>): Boolean = features.containsAll(elements)

        override fun isEmpty(): Boolean = features.isEmpty()

        override val size: Int
            get() = features.size
    }

    data class Unordered(
        override val features: Set<DiscreteFeature> = emptySet()
    ) : Schema(features), Set<DiscreteFeature> by features {

        constructor(vararg features: DiscreteFeature) : this(setOf(*features))

        override fun iterator(): Iterator<DiscreteFeature> = features.iterator()

        override fun contains(element: DiscreteFeature): Boolean = features.contains(element)

        override fun containsAll(elements: Collection<DiscreteFeature>): Boolean = features.containsAll(elements)

        override fun isEmpty(): Boolean = features.isEmpty()

        override val size: Int
            get() = features.size
    }

    object Empty : Schema(emptyList())

    companion object {
        const val serialVersionUID = 5329536256856403760L
    }
}
