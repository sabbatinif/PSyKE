package it.unibo.skpf.re.classification.real

import MapIntToRuleList
import flatten
import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.schema.Discretization
import it.unibo.skpf.re.utils.createHead
import it.unibo.skpf.re.utils.createTerm
import it.unibo.skpf.re.utils.createVariableList
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Term
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.theory.MutableTheory
import it.unibo.tuprolog.utils.Cache
import optimise
import smile.classification.Classifier
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.categories
import smile.data.inputsArray
import smile.data.name
import smile.data.type.StructType
import smile.data.writeColumn
import java.lang.IndexOutOfBoundsException
import kotlin.streams.toList

internal class REAL(
    override val predictor: Classifier<DoubleArray>,
    override val discretization: Discretization
) : Extractor<DoubleArray, Classifier<DoubleArray>> {

    private lateinit var ruleSet: MapIntToRuleList
    private val ruleSetCache: Cache<DataFrame, MapIntToRuleList> = Cache.simpleLru()

    private fun init(dataset: DataFrame): MapIntToRuleList {
        return dataset.categories().mapIndexed { i, _ ->
            i to mutableListOf<Rule>()
        }.toMap()
    }

    override fun extract(dataset: DataFrame): MutableTheory {
        this.ruleSet = ruleSetCache.getOrSet(dataset) { this.createRuleSet(dataset) }
        return this.createTheory(dataset)
    }

    private fun createRuleSet(dataset: DataFrame): MapIntToRuleList {
        val ruleSet = this.init(dataset)
        for (sample in dataset.inputsArray())
            ruleSet.getValue(this.predictor.predict(sample)).apply {
                if (!covers(dataset, sample, this)) {
                    this.add(createNewRule(dataset, sample))
                }
            }
        return ruleSet.also { it.optimise() }
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
        val mutableRule = rule.toMutableLists()
        var samples = DataFrame.of(arrayOf(sample), *dataset.names())
        rule.toLists().zip(mutableRule) { predicates, mutablePredicates ->
            for (predicate in predicates) {
                samples = removeAntecedents(samples, predicate, mutablePredicates)
            }
        }
        return Rule(mutableRule.first(), mutableRule.last())
    }

    private fun createTheory(dataset: DataFrame): MutableTheory {
        val variables = createVariableList(this.discretization)
        val theory = MutableTheory.empty()
        for ((key, rule) in ruleSet.flatten()) {
            theory.assertZ(createClause(dataset, variables, key, rule))
        }
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
        rule.toLists().zip(listOf(true, false)) { predicate, truthValue ->
            for (variable in predicate) {
                this.discretization.first { it.admissibleValues.containsKey(variable) }.apply {
                    body.add(createTerm(variables[this.name], this.admissibleValues[variable]!!, truthValue))
                }
            }
        }
        return body.toTypedArray()
    }

    private fun subset(x: DataFrame, feature: String): Pair<DataFrame, Boolean> {
        val all = x.writeColumn(feature, 0.0).union(x.writeColumn(feature, 1.0))
        return Pair(all, this.predictor.predict(all.toArray()).distinct().size == 1)
    }

    private fun covers(dataset: DataFrame, x: DoubleArray, rules: List<Rule>): Boolean =
        ruleFromExample(dataset.schema(), x).let { rule -> rules.any { rule.isSubRuleOf(it) } }

    private fun ruleFromExample(schema: StructType, x: DoubleArray): Rule {
        val t = mutableListOf<String>()
        val f = mutableListOf<String>()
        schema.fields().zip(x.toTypedArray()) { field, value ->
            (if (value == 1.0) t else f).add(field.name)
        }
        return Rule(t, f).reduce(this.discretization)
    }

    private fun predict(x: Tuple, schema: StructType): Int =
        ruleSet.flatten()
            .firstOrNull { ruleFromExample(schema, x.toDoubleArray(to = -2)).isSubRuleOf(it.second) }
            ?.first
            ?: -1

    private fun Tuple.toDoubleArray(from: Int = 0, to: Int = -1): DoubleArray {
        val lastIndex = when {
            to < 0 -> length() + to
            else -> to
        }
        if (from < 0) throw IndexOutOfBoundsException("Index $from is out of range")
        if (from > lastIndex) throw IndexOutOfBoundsException("Index $to is out of range")
        return DoubleArray(lastIndex - from + 1) { getDouble(from + it) }
    }

    override fun predict(dataset: DataFrame) =
        dataset.stream().map { this.predict(it, dataset.schema()) }.toList().toTypedArray()
}
