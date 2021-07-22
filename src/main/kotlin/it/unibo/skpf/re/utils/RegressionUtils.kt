package it.unibo.skpf.re.utils

import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.cart.CartPredictor
import it.unibo.skpf.re.utils.RegressionUtils.createCART
import it.unibo.skpf.re.utils.RegressionUtils.createGridEx
import it.unibo.skpf.re.utils.RegressionUtils.createITER
import it.unibo.skpf.re.utils.RegressionUtils.createSVR
import it.unibo.tuprolog.core.format
import it.unibo.tuprolog.theory.Theory
import org.apache.commons.csv.CSVFormat
import smile.base.mlp.Layer
import smile.base.mlp.LayerBuilder
import smile.base.mlp.OutputFunction
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
import smile.regression.rbfnet
import smile.regression.svr
import smile.validation.metric.MAD
import smile.validation.metric.MSE
import smile.validation.metric.R2

internal object MLPRegressorUtils {
    // mlp params
    private const val initLearningRate = 0.01
    private const val decaySteps = 10000.0
    private const val endLearningRate = 0.001
    private const val epochs = 200
    private const val layer1neurons = 35
    private const val layer2neurons = 15

    fun createMLP(x: Array<DoubleArray>, y: DoubleArray) =
        mlpRegressor(
            x, y,
            arrayOf(
                Layer.tanh(layer1neurons),
                Layer.tanh(layer2neurons),
                Layer.mse(1, OutputFunction.LINEAR)
            ),
            epochs,
            TimeFunction.linear(initLearningRate, decaySteps, endLearningRate)
        )

    private fun mlpRegressor(
        x: Array<DoubleArray>,
        y: DoubleArray,
        builders: Array<LayerBuilder>,
        epochs: Int,
        learningRate: TimeFunction,
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
        (1..epochs).forEach { _ -> net.update(x, y) }
        return net
    }
}

internal object RegressionUtils {
    // svr params
    private const val sigma = 0.06
    private const val epsilon = 0.005
    private const val C = 1.0

    fun createSVR(train: DataFrame) =
        svr(train.inputsArray(), train.outputsArray(), GaussianKernel(sigma), epsilon, C)

    // rbf params
    const val k = 95
    const val normalized = true

    fun createRBF(x: Array<DoubleArray>, y: DoubleArray) =
        rbfnet(x, y, k, normalized)

    // iter params
    const val minUpdate = 1.0 / 20
    const val threshold = 0.19

    fun createITER(model: Regression<DoubleArray>) =
        Extractor.iter(model, minUpdate = minUpdate, threshold = threshold)

    const val gridExThreshold = 0.005
    const val steps = 2

    fun createGridEx(model: Regression<DoubleArray>) =
        Extractor.gridex(
            model, steps = listOf(steps, steps), threshold = gridExThreshold, minExamples = 15
        )

    // cart params
    const val maxDepth = 3
    const val maxNodes = 0
    const val nodeSize = 5

    fun createCART(train: DataFrame) =
        CartPredictor(
            cart(
                Formula.lhs(train.name()),
                train.inputs().merge(train.outputs()),
                maxDepth, maxNodes, nodeSize
            )
        )
}

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

@Suppress("UNUSED_VARIABLE")
fun regression(name: String, testSplit: Double) {
    println("*** $name ***")
    val dataset = Read.csv("datasets/$name", CSVFormat.DEFAULT.withHeader())
    val (train, test) = dataset.randomSplit(testSplit)
//    val x = train.inputsArray()
//    val y = train.outputsArray()

    val svr = createSVR(train)
//    saveToFile("artiRBF95.txt", rbf)
//    saveToFile("artiTest50.txt", test)
//    saveToFile("artiTrain50.txt", train)

    printMetrics(svr.predict(test.inputsArray()), test.outputsArray())
    val iter = createITER(svr)
    testRegressionExtractor("ITER", train, test, iter, svr, true, true)
    val gridex = createGridEx(svr)
    testRegressionExtractor("GridEx", train, test, gridex, svr, true, true)
    val cart = createCART(train)
    val cartEx = Extractor.cart(cart)
    testRegressionExtractor("CART", train, test, cartEx, true, true)
}

private fun test(
    name: String,
    test: DataFrame,
    printMetrics: Boolean,
    theory: Theory,
    predictions: Array<*>
) {
    println("\n################################")
    println("# $name extractor")
    println("################################\n")
    if (printMetrics) {
        val metrics = printMetrics(
            test.outputsArray(),
            predictions.map { it.toString().toDouble() }.toDoubleArray(),
            false, false, false
        )
        println(
            theory.size.toString() +
                " rules with R2 = " + metrics.first +
                " and MSE = " + metrics.second + " w.r.t. the data"
        ).also { println() }
    }
}

fun testRegressionExtractor(
    name: String,
    train: DataFrame,
    test: DataFrame,
    extractor: Extractor<Tuple, CartPredictor>,
    printMetrics: Boolean = true,
    printRules: Boolean = false
) {
    val theory = extractor.extract(train)
    test(name, test, printMetrics, theory, extractor.predict(test))
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
    val theory = extractor.extract(train)
    test(name, test, printMetrics, theory, extractor.predict(test))
    if (printMetrics) {
        val metricsFid = printMetrics(
            predictor.predict(test.inputsArray()),
            extractor.predict(test).map { it.toString().toDouble() }.toDoubleArray(),
            false, false, false
        )
        println("R2 = ${metricsFid.first} and MSE = ${metricsFid.second} w.r.t. the black box")
            .also { println() }
    }
    if (printRules)
        theory.clauses.forEach { println(it.format(prettyRulesFormatter())) }.also { println() }
}
