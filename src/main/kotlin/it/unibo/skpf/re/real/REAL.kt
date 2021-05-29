package it.unibo.skpf.re.real

import it.unibo.skpf.re.*
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Term
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.theory.MutableTheory
import smile.classification.Classifier
import smile.data.*
import kotlin.streams.toList

internal class REAL(
    override val predictor: Classifier<DoubleArray>,
    override val featureSet: Set<BooleanFeatureSet>
) : Extractor<DoubleArray, Classifier<DoubleArray>> {

    private lateinit var dataset: DataFrame
    private lateinit var ruleSet: Map<Int, MutableList<Rule>>

    private fun init(dataset: DataFrame): Map<Int, MutableList<Rule>> {
        return mapOf(
            *dataset.categories().mapIndexed { i, _ ->
                i to mutableListOf<Rule>()
            }.toTypedArray()
        )
    }

    override fun extract(dataset: DataFrame): MutableTheory {
        var ruleSet = this.init(dataset)
        for (sample in dataset.inputsArray()) {
            val c = this.predictor.predict(sample)
            if (!this.covers(dataset, sample, ruleSet.getValue(c)))
                ruleSet.getValue(c).add(createNewRule(dataset, sample))
        }
        ruleSet = this.optimise(ruleSet)
        this.ruleSet = ruleSet
        this.dataset = dataset
        return this.createTheory(dataset, ruleSet)
    }

    private fun createNewRule(dataset: DataFrame, sample: DoubleArray): Rule {
        val rule = ruleFromExample(dataset, sample)
        return removeAntecedents(rule, dataset, sample)
    }

    private fun removeAntecedents(rule: Rule, dataset: DataFrame, sample: DoubleArray): Rule {
        val mutableRule = rule.asMutable()
        var samples = DataFrame.of(arrayOf(sample), *dataset.names())
        rule.asList().zip(mutableRule) { predicates, mutablePredicates ->
            for (predicate in predicates) {
                val ret = this.subset(samples, predicate)
                if (ret.second) {
                    mutablePredicates.remove(predicate)
                    samples = ret.first
                }
            }
        }
        return Rule(mutableRule.first(), mutableRule.last())
    }

    private fun createTheory(dataset: DataFrame, ruleSet: Map<Int, MutableList<Rule>>): MutableTheory {
        val variables = createVariableList(this.featureSet)
        val theory = MutableTheory.empty()
        for ((key, ruleList) in ruleSet)
            for (rule in ruleList)
                theory.assertZ(createClause(dataset, variables, key, rule))
        return theory
    }

    private fun createClause(dataset: DataFrame, variables: Map<String, Var>, key: Int, rule: Rule): Clause {
        val head = createHead(
            "concept", variables.values,
            dataset.categories().elementAt(key).toString()
        )
        return Clause.of(head, *createBody(variables, rule))
    }

    private fun createBody(variables: Map<String, Var>, rule: Rule): Array<Term> {
        val body: MutableList<Term> = mutableListOf()
        rule.asList().zip(listOf(true, false)) { predicate, truthValue ->
            for (variable in predicate)
                this.featureSet.first { it.set.containsKey(variable) }.apply {
                    body.add(createTerm(variables[this.name], this.set[variable], truthValue))
                }
        }
        return body.toTypedArray()
    }

    private fun subset(x: DataFrame, feature: String): Pair<DataFrame, Boolean> {
        val all = x.writeColumn(feature, 0.0)
            .union(x.writeColumn(feature, 1.0))
        return Pair(all, this.predictor.predict(all.toArray()).distinct().size == 1)
    }

    private fun covers(dataset: DataFrame, x: DoubleArray, rules: List<Rule>): Boolean {
        this.ruleFromExample(dataset, x).apply {
            for (rule in rules)
                if (this.subRule(rule))
                    return true
        }
        return false
    }

    private fun ruleFromExample(dataset: DataFrame, x: DoubleArray): Rule {
        val t = mutableListOf<String>()
        val f = mutableListOf<String>()

        dataset.schema().fields().zip(x.toTypedArray()) { field, value ->
            (if (value == 1.0) t else f).add(field.name)
        }
        return Rule(t, f).reduce(this.featureSet)
    }

    private fun predict(x: Tuple): Int =
        flat(this.ruleSet)
            .firstOrNull {
                ruleFromExample(this.dataset, tupleToArray(x))
                    .subRule(it.second) }?.first ?: -1

    private fun tupleToArray(x: Tuple) =
        sequence {
            for (i in 0 until x.length() - 1)
                yield(x.getDouble(i))
        }.toList().toDoubleArray()

    private fun flat(ruleSet: Map<Int, MutableList<Rule>>): List<Pair<Int, Rule>> =
        ruleSet.flatMap { (key, rules) -> rules.map { key to it } }

    override fun predict(dataset: DataFrame): IntArray =
        dataset.stream().map { this.predict(it) }.toList().toIntArray()

    private fun optimise(ruleSet: Map<Int, MutableList<Rule>>): Map<Int, MutableList<Rule>> {
        sequence {
            ruleSet.forEach { (key, rules) ->
                yieldAll(uselessRules(key, rules))
            }
        }.forEach { (key, rule) ->
            ruleSet.getValue(key).remove(rule)
        }
        return ruleSet
    }

    private fun uselessRules(key: Int, rules: List<Rule>): Sequence<Pair<Int, Rule>> =
        sequence {
            for (rule in rules)
                for (otherRule in rules.minus(rule))
                    if (otherRule.subRule(rule))
                        yield(Pair(key, otherRule))
        }
}