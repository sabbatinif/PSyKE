package it.unibo.skpf.re.cart

import it.unibo.skpf.re.*
import it.unibo.skpf.re.createHead
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Real
import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.theory.MutableTheory
import it.unibo.tuprolog.theory.Theory
import smile.base.cart.DecisionNode
import smile.base.cart.Node
import smile.base.cart.OrdinalNode
import smile.classification.DecisionTree
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.categories
import java.lang.IllegalStateException

typealias SplitList = List<NodeSplit>
typealias SplitSequence = Sequence<Pair<List<NodeSplit>, String>>

internal class CartExtractor(
    override val predictor: DecisionTree,
    override val featureSet: Collection<BooleanFeatureSet>
) : Extractor<Tuple, DecisionTree> {

    override fun extract(dataset: DataFrame): Theory {
        val root = this.predictor.root()
        return createTheory(root.asSequence(dataset))
    }

    private fun createTheory(splits: SplitSequence): Theory {
        val variables = createVariableList(this.featureSet)
        val theory = MutableTheory.empty()
        for (split in splits)
            theory.assertZ(
                Clause.of(
                createHead("concept", variables.values, split.second),
                *createBody(variables, split.first)
            ))
        return theory
    }

    private fun getVariable(feature: String, variables: Map<String, Var>): Var {
        return variables.getOrElse(
            featureSet.firstOrNull {
                it.set.containsKey(feature)
            }?.name ?: feature
        ) {
            throw IllegalStateException()
        }
    }

    private fun createBody(variables: Map<String, Var>, constraints: List<NodeSplit>) = sequence {
        for (constraint in constraints)
            yield(
                Struct.of(
                    if (constraint.positive) "le" else "gt",
                    getVariable(constraint.feature, variables),
                    Real.of(constraint.value)
                )
            )
    }.toList().toTypedArray()

    private fun subTree(node: OrdinalNode, dataset: DataFrame, constraints: SplitList): SplitSequence {
        val split = node.split(dataset.schema())
        return node.trueChild().asSequence(dataset, constraints.plus(split)).plus(
            node.falseChild().asSequence(dataset, constraints.plus(split.not()))
        )
    }

    private fun Node.asSequence(
        dataset: DataFrame, constraints: SplitList = emptyList(),
    ): SplitSequence = sequence {
        when(val node = this@asSequence) {
            is OrdinalNode -> yieldAll(subTree(node, dataset, constraints))
            is DecisionNode -> yield(constraints to dataset.categories().elementAt(node.output()).toString())
        }
    }

    override fun predict(dataset: DataFrame): Array<*> =
        this.predictor.predict(dataset).toTypedArray()
}