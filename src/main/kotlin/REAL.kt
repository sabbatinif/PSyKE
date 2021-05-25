import smile.classification.Classifier
import smile.data.DataFrame
import it.unibo.tuprolog.core.*
import it.unibo.tuprolog.theory.MutableTheory
import smile.data.Tuple
import javax.xml.crypto.Data
import kotlin.streams.toList

class REAL(override val predictor: Classifier<DoubleArray>,
           override val featureSet: Set<BooleanFeatureSet>
) : Extractor<DoubleArray, Classifier<DoubleArray>> {

    private lateinit var dataset: DataFrame
    private lateinit var ruleSet: Map<Int, MutableList<Rule>>

    private fun init(x: DataFrame) {
        this.dataset = x
        this.ruleSet = mapOf(
            *x.categories().mapIndexed {
                    i, _ -> i to mutableListOf<Rule>()
            }.toTypedArray()
        )
    }

    override fun extract(x: DataFrame): MutableTheory {
        this.init(x)

        for (sample in x.inputsArray()) {
            val c = this.predictor.predict(sample)
            if (!this.covers(sample, c))
            {
                val rule = ruleFromExample(sample)
                val mutablePair = listOf(
                    rule.truePred.toMutableList(),
                    rule.falsePred.toMutableList()
                )

                var samples = DataFrame.of(arrayOf(sample), *x.names())
                listOf(rule.truePred, rule.falsePred).zip(mutablePair) { it, mit ->
                    it.forEach {
                        val ret = this.subset(samples, it)
                        if (ret.second) {
                            mit.remove(it)
                            samples = ret.first
                        }
                    }
                }
                this.ruleSet.getValue(c).add(Rule(mutablePair.first(), mutablePair.last()))
            }
        }
        this.optimise()

        return this.createTheory()
    }

    override fun createTheory(): MutableTheory {
        val variables = createVariableList(this.featureSet)
        val theory = MutableTheory.empty()
        for ((key, ruleList) in this.ruleSet) {
            val head = createHead("concept", variables.values,
                this.dataset.categories().elementAt(key).toString())
            for (rule in ruleList) {
                val body: MutableList<Term> = mutableListOf()

                listOf(rule.truePred, rule.falsePred).zip(listOf(true, false)) { pred, cond ->
                    for (variable in pred) {
                        this.featureSet.first { it.set.containsKey(variable) }.apply {
                            body.add(
                                createTerm(
                                    variables[this.name] ?: Var.of(this.name),
                                    this.set[variable] ?: Any(),
                                    cond
                                )
                            )
                        }
                    }
                }
                theory.assertZ(Clause.of(head, *body.toTypedArray()))
            }
        }
        return theory
    }

    private fun subset(x: DataFrame, feature: String): Pair<DataFrame, Boolean> {
        val all = x.writeColumn(feature, 0.0)
            .union(x.writeColumn(feature, 1.0))
        return Pair(all, this.predictor.predict(all.toArray()).distinct().size == 1)
    }

    private fun covers(x: DoubleArray, y: Int): Boolean {
        this.ruleFromExample(x).apply {
            for (rule in ruleSet.getValue(y))
                if (this.subRule(rule))
                    return true
        }
        return false
    }

    private fun ruleFromExample(x: DoubleArray): Rule {
        val t = mutableListOf<String>()
        val f = mutableListOf<String>()

        this.dataset.schema().fields().zip(x.toTypedArray()) {
            field, value -> (if (value == 1.0) t else f).add(field.name)
        }
        return Rule(t, f).reduce(this.featureSet)
    }

    private fun predict(x: Tuple): Int {
        val data = mutableListOf<Double>()
        for (i in 0 until x.length() - 1) {
            data.add(x[i].toString().toDouble())
        }
        val rule = this.ruleFromExample(data.toDoubleArray())

        for ((key, rules) in this.ruleSet) {
            rules.forEach {
                if (rule.subRule(it))
                    return key
            }
        }
        return -1
    }

    override fun predict(x: DataFrame): IntArray {
        return x.stream().map { this.predict(it) }.toList().toIntArray()
    }

    private fun optimise() {
        val toRemove: MutableList<Pair<Int, Rule>> = mutableListOf()
        this.ruleSet.forEach { (key, rules) ->
            rules.forEach { r1 ->
                rules.minus(r1).forEach {r2 ->
                    if (r2.subRule(r1))
                        toRemove.add(Pair(key, r2))
                }
            }
        }
        toRemove.forEach { (key, rule) ->
            this.ruleSet.getValue(key).remove(rule)
        }
    }
}