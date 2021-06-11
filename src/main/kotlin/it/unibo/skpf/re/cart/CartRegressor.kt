package it.unibo.skpf.re.cart

import it.unibo.skpf.re.*
import it.unibo.skpf.re.createHead
import it.unibo.skpf.re.OriginalValue.Interval.GreaterThan
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.theory.MutableTheory
import it.unibo.tuprolog.theory.Theory
import smile.base.cart.DecisionNode
import smile.base.cart.Node
import smile.base.cart.OrdinalNode
import smile.base.cart.RegressionNode
import smile.classification.DecisionTree
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.categories
import smile.data.name
import smile.regression.RegressionTree

internal class CartRegressor(
    override val predictor: RegressionTree,
    override val featureSet: Collection<BooleanFeatureSet> = emptySet()
) : Extractor<Tuple, RegressionTree> {

    override fun extract(dataset: DataFrame): Theory {
        val root = this.predictor.root()
        return createTheory(root.asSequence(dataset), dataset)
    }

    private fun createTheory(leaves: LeafSequence, dataset: DataFrame): Theory {
        val variables = createVariableList(this.featureSet, dataset)
        val theory = MutableTheory.empty()
        for (leaf in leaves)
            theory.assertZ(
                Clause.of(
                createHead(dataset.name(), variables.values, leaf.second),
                *createBody(variables, leaf.first)
            ))
        return theory
    }

    private fun createBody(variables: Map<String, Var>, constraints: LeafConstraints) = sequence {
        for ((name, value) in constraints) {
            val feature = featureSet.firstOrNull { it.set.containsKey(name) }
            if (feature == null)
                yield(createTerm(variables[name], value))
            else
                yield(createTerm(variables[feature.name], feature.set[name]!!, value is GreaterThan))
        }
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
            is RegressionNode -> yield(constraints to node.output().toString())
        }
    }

    override fun predict(dataset: DataFrame): Array<*> =
        this.predictor.predict(dataset).toTypedArray()
}