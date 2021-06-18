package it.unibo.skpf.re.classification.real

import it.unibo.skpf.re.schema.DiscreteFeature

internal data class Rule(val truePredicates: List<String>, val falsePredicates: List<String>) {
    fun isSubRuleOf(rule: Rule): Boolean =
        truePredicates.containsAll(rule.truePredicates) && falsePredicates.containsAll((rule.falsePredicates))

    fun reduce(features: Iterable<DiscreteFeature>): Rule {
        val toBeRemoved = truePredicates.asSequence().flatMap { tp ->
            features.asSequence().filter { tp in it.admissibleValues }.flatMap { it.admissibleValues.keys }
        }.toSet()
        return Rule(this.truePredicates, falsePredicates.filterNot { it in toBeRemoved })
    }

    fun toMutableLists(): List<MutableList<String>> = this.toLists().map { it.toMutableList() }

    fun toLists(): List<List<String>> = listOf(this.truePredicates, this.falsePredicates)
}
