package it.unibo.skpf.re.schema

import java.io.Serializable

sealed class Discretization(
    protected open val features: Collection<DiscreteFeature>
) : Collection<DiscreteFeature> by features, Serializable {

    operator fun get(name: String): DiscreteFeature? =
        asSequence().firstOrNull { it.name == name }

    data class Ordered(
        override val features: List<DiscreteFeature> = emptyList()
    ) : Discretization(features), List<DiscreteFeature> by features {

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
    ) : Discretization(features), Set<DiscreteFeature> by features {

        constructor(vararg features: DiscreteFeature) : this(setOf(*features))

        override fun iterator(): Iterator<DiscreteFeature> = features.iterator()

        override fun contains(element: DiscreteFeature): Boolean = features.contains(element)

        override fun containsAll(elements: Collection<DiscreteFeature>): Boolean = features.containsAll(elements)

        override fun isEmpty(): Boolean = features.isEmpty()

        override val size: Int
            get() = features.size
    }

    object Empty : Discretization(emptyList())

    companion object {
        const val serialVersionUID = 5329536256856403760L
    }
}
