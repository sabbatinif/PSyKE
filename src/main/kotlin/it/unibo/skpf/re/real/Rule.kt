package it.unibo.skpf.re.real

import it.unibo.skpf.re.BooleanFeatureSet

internal class Rule(
    val truePredicates: List<String>,
    val falsePredicates: List<String>
) {
    fun subRule(rule: Rule): Boolean {
        return (this.truePredicates.containsAll(rule.truePredicates) &&
                this.falsePredicates.containsAll((rule.falsePredicates)))
    }

    fun reduce(featureSets: Set<BooleanFeatureSet>): Rule {
        val f = this.falsePredicates.toMutableList()
        for (variable in this.truePredicates)
            f.removeAll(
                featureSets.filter { featSet ->
                    variable in featSet.set.keys
                }.flatMap { it.set.keys }
            )
        return Rule(this.truePredicates, f)
    }

    fun asMutable() = listOf(
        this.truePredicates.toMutableList(),
        this.falsePredicates.toMutableList()
    )
}