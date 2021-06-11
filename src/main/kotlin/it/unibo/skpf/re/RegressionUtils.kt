package it.unibo.skpf.re

import it.unibo.tuprolog.core.format
import org.apache.commons.csv.CSVFormat
import smile.base.mlp.LayerBuilder
import smile.data.*
import smile.data.formula.Formula
import smile.io.Read
import smile.math.TimeFunction
import smile.regression.*

fun printMetrics(
    model: Regression<DoubleArray>,
    input: Array<DoubleArray>,
    expectedOutput: DoubleArray,
    r2: Boolean = true,
    mse: Boolean = true,
    mad: Boolean = false
) {
    val metric = model.metric(input, expectedOutput)
    if (r2)
        println("R2 = " + metric.RSquared)
    if (mse)
        println("MSE = " + metric.MSE)
    if (mad)
        println("MAD = " + metric.MAD)
}

fun regression(name: String, testSplit: Double) {
    println("*** $name ***")
    val dataset = Read.csv("datasets/$name", CSVFormat.DEFAULT.withHeader())
    val (train, test) = dataset.randomSplit(testSplit)
    val x = train.inputsArray()
    val y = train.outputsArray()
//    val mlp = MLPRegressor(
//        x, y,
//        arrayOf(Layer.tanh(35), Layer.tanh(15), Layer.mse(1, OutputFunction.LINEAR)),
//        200,
//        TimeFunction.linear(0.1, 10000.0, 0.05)
//    )
    val rbf = rbfnet(x, y, 95, true)
    printMetrics(rbf, test.inputsArray(), test.outputsArray())
    val cart = cart(
        Formula.lhs(train.name()),
        train.inputs().merge(train.outputs()),
        3, 0, 5
    )
    val cartEx = Extractor.cartRegression(cart)
    testRegressionExtractor("CART", train, test, cartEx, true, true)
}

fun testRegressionExtractor(
    name: String,
    train: DataFrame,
    test: DataFrame,
    extractor: Extractor<Tuple, RegressionTree>,
    metrics: Boolean,
    printRules: Boolean
) {
    val theory = extractor.extract(train)
    if (printRules)
        theory.clauses.forEach { println(it.format(prettyRulesFormatter())) }.also { println() }
}

fun MLPRegressor(
    x: Array<DoubleArray>, y: DoubleArray, builders: Array<LayerBuilder>, epochs: Int = 10,
    learningRate: TimeFunction = TimeFunction.linear(0.01, 10000.0, 0.001),
    momentum: TimeFunction = TimeFunction.constant(0.0),
    weightDecay: Double = 0.0, rho: Double = 0.0, epsilon: Double = 1E-7) : MLP {
    val net = MLP(x[0].size, *builders)
    net.setLearningRate(learningRate)
    net.setMomentum(momentum)
    net.setWeightDecay(weightDecay)
    net.setRMSProp(rho, epsilon)
    for (i in 1 .. epochs) net.update(x, y)
    return net
}
