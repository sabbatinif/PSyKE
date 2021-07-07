package it.unibo.skpf.re.classification.real

import IndexedRuleSet
import createIndexedRuleSet
import flatten
import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.schema.Discretization
import it.unibo.skpf.re.utils.createHead
import it.unibo.skpf.re.utils.createTerm
import it.unibo.skpf.re.utils.createVariableList
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Struct
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

    private lateinit var ruleSet: IndexedRuleSet

    private val ruleSetCache: Cache<DataFrame, IndexedRuleSet> = Cache.simpleLru()

    override fun extract(dataset: DataFrame): MutableTheory {
        ruleSet = ruleSetCache.getOrSet(dataset) { this.createRuleSet(dataset) }
        return this.createTheory(dataset, ruleSet)
    }

    private fun createRuleSet(dataset: DataFrame): IndexedRuleSet {
        val ruleSet = createIndexedRuleSet(dataset)
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

    // orrore
    // qui abbiamo il solito data set, il nome di una feature
    // e infine una regola, ovvero una lista di nomi di feature
    // (in realtà è mezza regola, può essere sia la lista di atomi
    // veri che la lista di atomi falsi)
    private fun removeAntecedents(
        samples: DataFrame,
        predicate: String,
        mutablePredicates: MutableList<String>
    ): DataFrame {

        val (dataframe, isSubset) = this.subset(samples, predicate)
        // quando questo è true, vuol dire che il predicato (ovvero
        // la feature) sia quando è 0 che quando è 1 non influisce
        // sulla classificazione, quindi è inutile...
        if (isSubset) {
            // e quindi va rimossa
            mutablePredicates.remove(predicate)
            // questo è il nuovo dataframe che viene creato nella funzione
            // issubset (vedi commenti in quella funzione)
            return dataframe
        }
        // se invece la condizione è falsa non succede nulla
        return samples
    }

    // qui prende in input una regola, un dataset e un singolo esempio
    private fun generalise(rule: Rule, dataset: DataFrame, sample: DoubleArray): Rule {
        // prima di tutto ho nececssità di modificare la regola togliendo atomi se serve
        val mutableRule = rule.toMutableLists()
        // poi ho bisogno di convertire il sample in un dataframe
        var samples = DataFrame.of(arrayOf(sample), *dataset.names())
        // a questo punto, per ogni predicato controllo se lo posso togliere
        // ma non posso iterare su una lista mentre la spupazzo, quindi
        // itero sulla lista e tolgo dalla lista mutabile "appaiata"
        rule.toLists().zip(mutableRule) { predicates, mutablePredicates ->
            // mano a mano che si cicla e vengono rimossi predicati/features,
            // il dataframe cresce (per ogni predicato tolto raddoppia la dimensione
            // perché viene copiato sia col predicato a 0 che a 1)
            // se non viene tolto niente, è come samples = samples
            for (predicate in predicates) {
                samples = removeAntecedents(samples, predicate, mutablePredicates)
            }
        }
        return Rule(mutableRule.first(), mutableRule.last())
    }

    private fun createTheory(dataset: DataFrame, ruleSet: IndexedRuleSet): MutableTheory {
        val theory = MutableTheory.empty()
        for ((key, rule) in ruleSet.flatten()) {
            val variables = createVariableList(this.discretization)
            theory.assertZ(createClause(dataset, variables, key, rule))
        }
        return theory
    }

    private fun createClause(dataset: DataFrame, variables: Map<String, Var>, key: Int, rule: Rule): Clause {
        val head = createHead(
            dataset.name(),
            variables.values,
            dataset.categories().elementAt(key).toString()
        )
        return Clause.of(head, createBody(variables, rule))
    }

    private fun createBody(variables: Map<String, Var>, rule: Rule): Sequence<Struct> =
        rule.toLists().asSequence().zip(sequenceOf(true, false)).flatMap { (predicates, truthValue) ->
            predicates.asSequence().map { variable ->
                discretization.firstOrNull { variable in it.admissibleValues }?.let {
                    createTerm(variables[it.name], it.admissibleValues[variable]!!, truthValue)
                }
            }
        }.filterNotNull()

    // x è un data set, feature è il nome di una feature
    // il nome subset l'ho preso dal paper di real, ma credo che di refactoring
    // in refactoring, alla fine la funzione che fa il subset è finita altrove
    // e io non me ne sono accorto xD
    private fun subset(x: DataFrame, feature: String): Pair<DataFrame, Boolean> {
        // all è la concatenazione di due data set, il primo è x
        // ma con la feature specificata in ingresso messa a 0, il secondo
        // è x con la feature a 1
        val all = x.writeColumn(feature, 0.0).union(x.writeColumn(feature, 1.0))
        // restituisce il data set concatenato e un booleanoc he indica se
        // tutti i samples generati nella riga sopra hanno la stessa classe
        return all to (predictor.predict(all.toArray()).distinct().size == 1)
    }

    private fun covers(dataset: DataFrame, x: DoubleArray, rules: List<Rule>): Boolean =
        ruleFromExample(dataset.schema(), x).let { rule -> rules.any { rule.isSubRuleOf(it) } }

    private fun ruleFromExample(schema: StructType, x: DoubleArray): Rule {
        val truePredicates = mutableListOf<String>()
        val falsePredicates = mutableListOf<String>()
        val fieldsIter = schema.fields().iterator()
        val xIter = x.iterator()
        while (fieldsIter.hasNext() && xIter.hasNext()) {
            val field = fieldsIter.next()
            val value = xIter.nextDouble()
            if (value == 1.0) {
                truePredicates.add(field.name)
            } else {
                falsePredicates.add(field.name)
            }
        }
        return Rule(truePredicates, falsePredicates).reduce(this.discretization)
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
