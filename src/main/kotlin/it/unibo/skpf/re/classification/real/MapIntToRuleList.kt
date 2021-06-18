import it.unibo.skpf.re.classification.real.Rule
import smile.data.DataFrame
import smile.data.categories

internal typealias IndexedRuleSet = Map<Int, MutableList<Rule>>

internal fun IndexedRuleSet.flatten(): Sequence<Pair<Int, Rule>> =
    asSequence().flatMap { (key, rules) -> rules.map { key to it } }

internal fun IndexedRuleSet.optimise() {
    val uselessRules = this.flatMap(::uselessRules)
    for ((key, rule) in uselessRules) {
        getValue(key).remove(rule)
    }
}

internal fun createIndexedRuleSet(dataframe: DataFrame): IndexedRuleSet =
    dataframe.categories().mapIndexed { i, _ -> i to mutableListOf<Rule>() }.toMap()

private fun uselessRules(keyRules: Map.Entry<Int, List<Rule>>): Sequence<Pair<Int, Rule>> =
    uselessRules(keyRules.key, keyRules.value)

private fun uselessRules(key: Int, rules: Iterable<Rule>): Sequence<Pair<Int, Rule>> =
    rules.asSequence().flatMap { rule ->
        rules.filterNot { it == rule }.filter { it.isSubRuleOf(rule) }
    }.map { key to it }
