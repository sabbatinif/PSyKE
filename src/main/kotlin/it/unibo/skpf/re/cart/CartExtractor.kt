package it.unibo.skpf.re.cart

import it.unibo.skpf.re.*
import it.unibo.skpf.re.createHead
import it.unibo.tuprolog.core.Clause
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

typealias LeafConstraints = List<Pair<String, OriginalValue>>
typealias LeafSequence = Sequence<Pair<LeafConstraints, String>>

internal class CartExtractor(
    override val predictor: DecisionTree,
    override val featureSet: Collection<BooleanFeatureSet>
) : Extractor<Tuple, DecisionTree> {

    override fun extract(dataset: DataFrame): Theory {
        val root = this.predictor.root()
        return createTheory(root.asSequence(dataset))
    }

    private fun createTheory(leaves: LeafSequence): Theory {
        val variables = createVariableList(this.featureSet)
        val theory = MutableTheory.empty()
        for (leaf in leaves)
            theory.assertZ(
                Clause.of(
                createHead("concept", variables.values, leaf.second),
                *createBody(variables, leaf.first)
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

    private fun createBody(variables: Map<String, Var>, constraints: LeafConstraints) = sequence {
        for (constraint in constraints)
            yield(
                createTerm(getVariable(constraint.first, variables), constraint.second)
            )
    }.toList().toTypedArray()

    private fun subTree(node: OrdinalNode, dataset: DataFrame, constraints: LeafConstraints): LeafSequence {
        val split = node.split(dataset.schema())
        return node.trueChild().asSequence(dataset, constraints.plus(split.first)).plus(
            node.falseChild().asSequence(dataset, constraints.plus(split.second))
        )
    }

    private fun Node.asSequence(
        dataset: DataFrame, constraints: LeafConstraints = emptyList(),
    ): LeafSequence = sequence {
        when(val node = this@asSequence) {
            is OrdinalNode -> yieldAll(subTree(node, dataset, constraints))
            is DecisionNode -> yield(constraints to dataset.categories().elementAt(node.output()).toString())
        }
    }

    override fun predict(dataset: DataFrame): Array<*> =
        this.predictor.predict(dataset).toTypedArray()
}