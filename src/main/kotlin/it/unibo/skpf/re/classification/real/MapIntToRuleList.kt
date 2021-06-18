import it.unibo.skpf.re.classification.real.Rule

internal typealias MapIntToRuleList = Map<Int, MutableList<Rule>>

internal fun MapIntToRuleList.flatten(): Sequence<Pair<Int, Rule>> =
    asSequence().flatMap { (key, rules) -> rules.map { key to it } }

internal fun MapIntToRuleList.optimise() {
    val uselessRules = this.flatMap(::uselessRules)
    for ((key, rule) in uselessRules) {
        getValue(key).remove(rule)
    }
}

private fun uselessRules(keyRules: Map.Entry<Int, List<Rule>>): Sequence<Pair<Int, Rule>> =
    uselessRules(keyRules.key, keyRules.value)

private fun uselessRules(key: Int, rules: Iterable<Rule>): Sequence<Pair<Int, Rule>> =
    rules.asSequence().flatMap { rule ->
        rules.filterNot { it == rule }.filter { it.isSubRuleOf(rule) }
    }.map { key to it }
