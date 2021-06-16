package it.unibo.skpf.re.utils

import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.cart.CartPredictor
import it.unibo.tuprolog.core.format
import org.apache.commons.csv.CSVFormat
import smile.base.mlp.LayerBuilder
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.formula.Formula
import smile.data.inputs
import smile.data.inputsArray
import smile.data.name
import smile.data.outputs
import smile.data.outputsArray
import smile.data.randomSplit
import smile.io.Read
import smile.math.TimeFunction
import smile.math.kernel.GaussianKernel
import smile.regression.MLP
import smile.regression.Regression
import smile.regression.cart
import smile.regression.svr
import smile.validation.metric.MAD
import smile.validation.metric.MSE
import smile.validation.metric.R2

fun printMetrics(
    actual: DoubleArray,
    expected: DoubleArray,
    printR2: Boolean = true,
    printMSE: Boolean = true,
    printMAD: Boolean = false
): Triple<Double, Double, Double> {
    val r2 = R2.of(expected, actual).round(2)
    val mse = MSE.of(expected, actual).round(2)
    val mad = MAD.of(expected, actual).round(2)
    if (printR2)
        println("Regressor R2 = $r2")
    if (printMSE)
        println("Regressor MSE = $mse")
    if (printMAD)
        println("Regressor MAD = $mad")
    return Triple(r2, mse, mad)
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

//    val rbf = rbfnet(x, y, 95, true)

    val svr = svr(train.inputsArray(), train.outputsArray(), GaussianKernel(0.06), 0.005, 1.0)
//    saveToFile("artiRBF95.txt", rbf)
//    saveToFile("artiTest50.txt", test)
//    saveToFile("artiTrain50.txt", train)

    printMetrics(svr.predict(test.inputsArray()), test.outputsArray())
    val iter = Extractor.iter(svr, minUpdate = (1.0 / 8), threshold = 0.18)
    testRegressionExtractor("ITER", train, test, iter, svr, true, true)
    val cart = CartPredictor(
        cart(
            Formula.lhs(train.name()),
            train.inputs().merge(train.outputs()),
            3, 0, 5
        )
    )
    val cartEx = Extractor.cart(cart)
    testRegressionExtractor("CART", train, test, cartEx, true, true)
}

fun testRegressionExtractor(
    name: String,
    train: DataFrame,
    test: DataFrame,
    extractor: Extractor<Tuple, CartPredictor>,
    printMetrics: Boolean = true,
    printRules: Boolean = false
) {
    println("\n################################")
    println("# $name extractor")
    println("################################\n")
    val theory = extractor.extract(train)
    if (printMetrics) {
        val metrics = printMetrics(
            test.outputsArray(),
            extractor.predict(test).map { it.toString().toDouble() }.toDoubleArray(),
            false, false, false
        )
        println(
            theory.size.toString() +
                " rules with R2 = " + metrics.first +
                " and MSE = " + metrics.second + " w.r.t. the data"
        ).also { println() }
    }
    if (printRules)
        theory.clauses.forEach { println(it.format(prettyRulesFormatter())) }.also { println() }
}

fun testRegressionExtractor(
    name: String,
    train: DataFrame,
    test: DataFrame,
    extractor: Extractor<*, *>,
    predictor: Regression<DoubleArray>,
    printMetrics: Boolean = true,
    printRules: Boolean = false
) {
    println("\n################################")
    println("# $name extractor")
    println("################################\n")
    val theory = extractor.extract(train)
    if (printMetrics) {
        val metrics = printMetrics(
            test.outputsArray(),
            extractor.predict(test).map { it.toString().toDouble() }.toDoubleArray(),
            false, false, false
        )
        println(
            theory.size.toString() +
                " rules with R2 = " + metrics.first +
                " and MSE = " + metrics.second + " w.r.t. the data"
        )
        val metricsFid = printMetrics(
            predictor.predict(test.inputsArray()),
            extractor.predict(test).map { it.toString().toDouble() }.toDoubleArray(),
            false, false, false
        )
        println(
            "R2 = " + metricsFid.first +
                " and MSE = " + metricsFid.second + " w.r.t. the black box"
        ).also { println() }
    }
    if (printRules)
        theory.clauses.forEach { println(it.format(prettyRulesFormatter())) }.also { println() }
}

fun MLPRegressor(
    x: Array<DoubleArray>,
    y: DoubleArray,
    builders: Array<LayerBuilder>,
    epochs: Int = 10,
    learningRate: TimeFunction = TimeFunction.linear(0.01, 10000.0, 0.001),
    momentum: TimeFunction = TimeFunction.constant(0.0),
    weightDecay: Double = 0.0,
    rho: Double = 0.0,
    epsilon: Double = 1E-7
): MLP {
    val net = MLP(x[0].size, *builders)
    net.setLearningRate(learningRate)
    net.setMomentum(momentum)
    net.weightDecay = weightDecay
    net.setRMSProp(rho, epsilon)
    for (i in 1..epochs) net.update(x, y)
    return net
}
