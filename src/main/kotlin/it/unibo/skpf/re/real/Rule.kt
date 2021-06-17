package it.unibo.skpf.re.real

import it.unibo.skpf.re.Feature

internal class Rule(
    val truePredicates: List<String>,
    val falsePredicates: List<String>
) {
    fun subRule(rule: Rule): Boolean {
        return (
            this.truePredicates.containsAll(rule.truePredicates) &&
                this.falsePredicates.containsAll((rule.falsePredicates))
            )
    }

    fun reduce(features: Collection<Feature>): Rule {
        val f = this.falsePredicates.toMutableList()
        for (variable in this.truePredicates)
            f.removeAll(
                features.filter { featSet ->
                    variable in featSet.set.keys
                }.map { it.set.keys }.flatten()
            )
        return Rule(this.truePredicates, f)
    }

    fun asMutable() = this.asList().map { it.toMutableList() }

    fun asList() = listOf(
        this.truePredicates,
        this.falsePredicates
    )
}
