package it.unibo.skpf.re.cart

import smile.base.cart.CART
import smile.base.cart.Node
import smile.classification.DataFrameClassifier
import smile.classification.DecisionTree
import smile.data.DataFrame
import smile.data.Tuple
import smile.regression.DataFrameRegression
import smile.regression.RegressionTree
import java.lang.IllegalStateException
import java.util.function.ToDoubleFunction

class CartPredictor(private val predictor: CART) : ToDoubleFunction<Tuple> {

    override fun applyAsDouble(value: Tuple?): Double =
        when (predictor) {
            is DecisionTree -> predictor.applyAsDouble(value)
            is RegressionTree -> predictor.applyAsDouble(value)
            else -> throw IllegalStateException()
        }

    fun root(): Node = predictor.root()

    fun predict(data: DataFrame): Array<*> =
        when (predictor) {
            is DecisionTree -> predictor.predict(data).toTypedArray()
            is RegressionTree -> predictor.predict(data).toTypedArray()
            else -> throw IllegalStateException()
        }
}