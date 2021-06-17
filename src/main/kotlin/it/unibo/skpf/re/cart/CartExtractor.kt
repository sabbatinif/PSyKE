package it.unibo.skpf.re.cart

import it.unibo.skpf.re.Feature
import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.OriginalValue.Interval.GreaterThan
import it.unibo.skpf.re.utils.TypeNotAllowedException
import it.unibo.skpf.re.utils.createHead
import it.unibo.skpf.re.utils.createTerm
import it.unibo.skpf.re.utils.createVariableList
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.theory.MutableTheory
import it.unibo.tuprolog.theory.Theory
import smile.base.cart.DecisionNode
import smile.base.cart.Node
import smile.base.cart.OrdinalNode
import smile.base.cart.RegressionNode
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.categories
import smile.data.name

internal class CartExtractor(
    override val predictor: CartPredictor,
    override val feature: Collection<Feature>
) : Extractor<Tuple, CartPredictor> {

    override fun extract(dataset: DataFrame): Theory {
        val root = this.predictor.root()
        return createTheory(root.asSequence(dataset), dataset)
    }

    private fun createTheory(leaves: LeafSequence, dataset: DataFrame): Theory {
        val variables = createVariableList(this.feature, dataset)
        val theory = MutableTheory.empty()
        for ((name, value) in leaves)
            theory.assertZ(
                Clause.of(
                    when (value) {
                        is Number -> createHead(dataset.name(), variables.values, value)
                        is String -> createHead(dataset.name(), variables.values, value)
                        else -> throw TypeNotAllowedException(value.javaClass.toString())
                    },
                    *createBody(variables, name)
                )
            )
        return theory
    }

    private fun createBody(variables: Map<String, Var>, constraints: LeafConstraints) = sequence {
        for ((name, value) in constraints) {
            val feature = feature.firstOrNull { it.set.containsKey(name) }
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
        dataset: DataFrame,
        constraints: LeafConstraints = emptyList(),
    ): LeafSequence = sequence {
        when (val node = this@asSequence) {
            is OrdinalNode -> yieldAll(subTree(node, dataset, constraints))
            is DecisionNode -> yield(constraints to dataset.categories().elementAt(node.output()).toString())
            is RegressionNode -> yield(constraints to node.output())
        }
    }

    override fun predict(dataset: DataFrame): Array<*> =
        this.predictor.predict(dataset)
}
