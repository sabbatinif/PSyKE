package it.unibo.skpf.re.real

import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.schema.Schema
import it.unibo.skpf.re.utils.createHead
import it.unibo.skpf.re.utils.createTerm
import it.unibo.skpf.re.utils.createVariableList
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Term
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.theory.MutableTheory
import it.unibo.tuprolog.utils.Cache
import smile.classification.Classifier
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.categories
import smile.data.inputsArray
import smile.data.name
import smile.data.type.StructType
import smile.data.writeColumn
import kotlin.streams.toList

private typealias MapIntToRuleList = Map<Int, MutableList<Rule>>

internal class REAL(
    override val predictor: Classifier<DoubleArray>,
    override val schema: Schema
) : Extractor<DoubleArray, Classifier<DoubleArray>> {

    private lateinit var ruleSet: MapIntToRuleList
    private val ruleSetCache: Cache<DataFrame, MapIntToRuleList> = Cache.simpleLru()

    private fun init(dataset: DataFrame): MapIntToRuleList {
        return mapOf(
            *dataset.categories().mapIndexed { i, _ ->
                i to mutableListOf<Rule>()
            }.toTypedArray()
        )
    }

    override fun extract(dataset: DataFrame): MutableTheory {
        this.ruleSet = ruleSetCache.getOrSet(dataset) { this.createRuleSet(dataset) }
        return this.createTheory(dataset)
    }

    private fun createRuleSet(dataset: DataFrame): MapIntToRuleList {
        val ruleSet = this.init(dataset)
        for (sample in dataset.inputsArray())
            ruleSet.getValue(this.predictor.predict(sample)).apply {
                if (!covers(dataset, sample, this))
                    this.add(createNewRule(dataset, sample))
            }
        return this.optimise(ruleSet)
    }

    private fun createNewRule(dataset: DataFrame, sample: DoubleArray): Rule {
        val rule = ruleFromExample(dataset.schema(), sample)
        return generalise(rule, dataset, sample)
    }

    private fun removeAntecedents(
        samples: DataFrame,
        predicate: String,
        mutablePredicates: MutableList<String>
    ): DataFrame {
        val ret = this.subset(samples, predicate)
        if (ret.second) {
            mutablePredicates.remove(predicate)
            return ret.first
        }
        return samples
    }

    private fun generalise(rule: Rule, dataset: DataFrame, sample: DoubleArray): Rule {
        val mutableRule = rule.asMutable()
        var samples = DataFrame.of(arrayOf(sample), *dataset.names())
        rule.asList().zip(mutableRule) { predicates, mutablePredicates ->
            for (predicate in predicates)
                samples = removeAntecedents(samples, predicate, mutablePredicates)
        }
        return Rule(mutableRule.first(), mutableRule.last())
    }

    private fun createTheory(dataset: DataFrame): MutableTheory {
        val variables = createVariableList(this.schema)
        val theory = MutableTheory.empty()
        for ((key, rule) in flat(ruleSet))
            theory.assertZ(createClause(dataset, variables, key, rule))
        return theory
    }

    private fun createClause(dataset: DataFrame, variables: Map<String, Var>, key: Int, rule: Rule): Clause {
        val head = createHead(
            dataset.name(), variables.values,
            dataset.categories().elementAt(key).toString()
        )
        return Clause.of(head, *createBody(variables, rule))
    }

    private fun createBody(variables: Map<String, Var>, rule: Rule): Array<Term> {
        val body: MutableList<Term> = mutableListOf()
        rule.asList().zip(listOf(true, false)) { predicate, truthValue ->
            for (variable in predicate)
                this.schema.first { it.admissibleValues.containsKey(variable) }.apply {
                    body.add(createTerm(variables[this.name], this.admissibleValues[variable]!!, truthValue))
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
        this.ruleFromExample(dataset.schema(), x).apply {
            for (rule in rules)
                if (this.subRule(rule))
                    return true
        }
        return false
    }

    private fun ruleFromExample(schema: StructType, x: DoubleArray): Rule {
        val t = mutableListOf<String>()
        val f = mutableListOf<String>()
        schema.fields().zip(x.toTypedArray()) { field, value ->
            (if (value == 1.0) t else f).add(field.name)
        }
        return Rule(t, f).reduce(this.schema)
    }

    private fun predict(x: Tuple, schema: StructType): Int =
        flat(this.ruleSet)
            .firstOrNull {
                ruleFromExample(schema, tupleToArray(x))
                    .subRule(it.second)
            }?.first ?: -1

    private fun tupleToArray(x: Tuple) =
        sequence {
            for (i in 0 until x.length() - 1)
                yield(x.getDouble(i))
        }.toList().toDoubleArray()

    private fun flat(ruleSet: MapIntToRuleList) =
        ruleSet.map { (key, rules) ->
            rules.map { key to it }
        }.flatten()

    override fun predict(dataset: DataFrame) =
        dataset.stream().map { this.predict(it, dataset.schema()) }.toList().toTypedArray()

    private fun optimise(ruleSet: MapIntToRuleList): MapIntToRuleList {
        val uselessRules = mutableListOf<Pair<Int, Rule>>()
        for ((key, rules) in ruleSet)
            uselessRules.addAll(uselessRules(key, rules))
        for ((key, rule) in uselessRules)
            ruleSet.getValue(key).remove(rule)
        return ruleSet
    }

    private fun uselessRules(key: Int, rules: List<Rule>): List<Pair<Int, Rule>> {
        val uselessRules = mutableListOf<Pair<Int, Rule>>()
        for (rule in rules)
            for (otherRule in rules.minus(rule))
                if (otherRule.subRule(rule))
                    uselessRules.add(Pair(key, otherRule))
        return uselessRules
    }
}
