import smile.classification.Classifier
import smile.data.DataFrame
import BooleanDataFrame.BooleanFeatureSet
import it.unibo.tuprolog.core.*
import it.unibo.tuprolog.theory.MutableTheory

class REAL(override val predictor: Classifier<DoubleArray>,
           override val dataset: DataFrame,
           val featureSets: MutableSet<BooleanFeatureSet>
) : Extractor<DoubleArray, Classifier<DoubleArray>> {

    class Rule(
        val truePred: List<String>,
        val falsePred: List<String>
    ) {
        fun subRule(rule: Rule): Boolean {
            return (this.truePred.containsAll(rule.truePred) &&
                    this.falsePred.containsAll((rule.falsePred)))
        }

        fun reduce(featureSets: MutableSet<BooleanFeatureSet>): Rule {
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

    val ruleSet: Map<Int, MutableList<Rule>>

    init {
        this.ruleSet = mapOf(
            *dataset.categories().mapIndexed {
                i, _ -> i to mutableListOf<Rule>()
            }.toTypedArray()
        )
    }

    override fun extract(x: Array<DoubleArray>): MutableTheory {
        for (sample in x) {
            val c = this.predictor.predict(sample)
            if (!this.covers(sample, c))
            {
                val rule = ruleFromExample(sample)
                val mutablePair = listOf(
                    rule.truePred.toMutableList(),
                    rule.falsePred.toMutableList()
                )

                var samples = DataFrame.of(arrayOf(sample), *this.dataset.names())
                listOf(rule.truePred, rule.falsePred).zip(mutablePair) { it, mit ->
                    it.forEach {
                        val ret = this.subset(samples, it)
                        if (ret.second) {
                            mit.remove(it)
                            samples = ret.first
                        }
                    }
                }
                this.ruleSet[c]!!.add(
                    Rule(mutablePair.first(), mutablePair.last()).reduce(this.featureSets)
                )
            }
        }
        this.optimise()

        return this.createTheory()
    }

    private fun createTheory(): MutableTheory {
        fun createTerm(v: Var, bounds: Pair<Double, Double>, positive: Boolean = true): Term
        {
            val functor = (if (!positive) "not_" else "") + "in"
            return Struct.of(functor, v, Real.of(bounds.first), Real.of(bounds.second))
        }

        val variables = mapOf(*this.featureSets.map {
            it.name to Var.of(it.name)
        }.toTypedArray())

        val theory = MutableTheory.empty()
        for ((key, ruleList) in this.ruleSet) {
            val head = Struct.of(
                "concept",
                variables.values.plus(Atom.of(key.toString()))
            )
            for (rule in ruleList) {
                val body: MutableList<Term> = mutableListOf()

                for ((pred, cond) in
                     listOf(Pair(rule.truePred, true), Pair(rule.falsePred, false))) {
                        for (variable in pred) {
                            this.featureSets
                                .filter { it.set.containsKey(variable) }.first()
                                .apply {
                                    body.add(
                                        createTerm(
                                            variables[this.name] ?: Var.of(this.name),
                                            this.set[variable] ?: Pair(0.0, 0.0),
                                            cond
                                        )
                                    )
                                }
                        }
                    }
                theory.assertZ(
                    Clause.of(
                        head,
                        *body.toTypedArray()
                    )
                )
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
            for (rule in ruleSet[y]!!)
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
        return Rule(t, f)
    }

    fun predict(x: DoubleArray): Int {
        val rule = this.ruleFromExample(x)

        for ((key, rules) in this.ruleSet) {
            rules.forEach {
                if (rule.subRule(it))
                    return key
            }
        }
        return -1
    }

    fun predict(x: Array<DoubleArray>): IntArray {
        return x.map { this.predict(it) }.toIntArray()
    }

    private fun optimise() {
        this.ruleSet.forEach { (key, rules) ->
            rules.forEach { r1 ->
                rules.minus(r1).forEach {r2 ->
                    if (r2.subRule(r1))
                        this.ruleSet[key]!!.remove(r2)
                }
            }
        }
    }
}