package it.unibo.skpf.re

class Rule(
    val truePred: List<String>,
    val falsePred: List<String>
) {
    fun subRule(rule: Rule): Boolean {
        return (this.truePred.containsAll(rule.truePred) &&
                this.falsePred.containsAll((rule.falsePred)))
    }

    fun reduce(featureSets: Set<BooleanFeatureSet>): Rule {
        val f = this.falsePred.toMutableList()

        for (variable in this.truePred) {
            f.removeAll(
                featureSets.filter { featSet ->
                    variable in featSet.set.keys
                }.flatMap { it.set.keys }
            )
        }
        return Rule(this.truePred, f)
    }
}