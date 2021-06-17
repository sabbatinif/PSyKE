package it.unibo.skpf.re.cart

import it.unibo.skpf.re.utils.TypeNotAllowedException
import smile.base.cart.CART
import smile.base.cart.Node
import smile.classification.DecisionTree
import smile.data.DataFrame
import smile.data.Tuple
import smile.regression.RegressionTree
import java.util.function.ToDoubleFunction

class CartPredictor(private val predictor: CART) : ToDoubleFunction<Tuple> {

    override fun applyAsDouble(value: Tuple?): Double =
        when (predictor) {
            is DecisionTree -> predictor.applyAsDouble(value)
            is RegressionTree -> predictor.applyAsDouble(value)
            else -> throw TypeNotAllowedException(predictor.javaClass.toString())
        }

    /**
     * Root node of the tree predictor.
     * @return the rood node of the tree predictor.
     */
    fun root(): Node = predictor.root()

    /**
     * Predicts the supplied instances.
     * @param data is the DataFrame to predict.
     * @return an Array of predictions.
     */
    fun predict(data: DataFrame): Array<*> =
        when (predictor) {
            is DecisionTree -> predictor.predict(data).toTypedArray()
            is RegressionTree -> predictor.predict(data).toTypedArray()
            else -> throw TypeNotAllowedException(predictor.javaClass.toString())
        }
}
