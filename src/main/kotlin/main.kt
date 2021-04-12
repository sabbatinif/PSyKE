import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Integer
import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.theory.MutableTheory
import it.unibo.tuprolog.theory.Theory
import smile.base.mlp.Layer
import smile.base.mlp.OutputFunction
import smile.classification.*
import smile.math.TimeFunction
import smile.math.kernel.GaussianKernel
import smile.math.kernel.MercerKernel
import smile.validation.Hyperparameters
import smile.validation.metric.Accuracy

fun main() {
    val dataset = Data("iris.data")
    val x = dataset.xtrain
    val y = dataset.ytrain.map { it.toInt() }.toIntArray()

    val model: MLP = mlp(
        x, y,
        arrayOf(Layer.rectifier(25), Layer.mle(3, OutputFunction.SOFTMAX)),
        450,
        TimeFunction.linear(0.05, 10000.0, 0.05))

    val knn = knn(x, y, 3)
    val log = logit(x, y)
    val rbf = rbfnet(x, y, 15)

    val models = listOf(model, knn, log, rbf)

    models.forEach { mod ->
        println(Accuracy.of(dataset.ytest.map { it.toInt() }.toIntArray(), mod.predict(dataset.xtest)))
    }

    models.forEach { mod ->
        dataset.xtest.forEach { print("${mod.predict(it)} ") }
        println()
    }
    dataset.ytest.forEach { print("${it.toInt()} ") }
    println()
/*
    val arti = Data("arti.csv")
    val model2 = rbfnet(
        arti.xtrain,
        arti.ytrain.map { it.toDouble() }.toDoubleArray(),
        4,
        true)
    println(model2.metric(arti.xtest, arti.ytest.map { it.toDouble() }.toDoubleArray()).MAD)

    val model3 = regressor(
        arti.xtrain,
        arti.ytrain.map { it.toDouble() }.toDoubleArray(),
        arrayOf(Layer.tanh(35), Layer.tanh(15), Layer.mse(1, OutputFunction.LINEAR)),
        200,
        TimeFunction.linear(0.1, 10000.0, 0.05))

    println(model3.metric(arti.xtest, arti.ytest.map { it.toDouble() }.toDoubleArray()).MAD)

    val theory = MutableTheory.empty()
    theory.assertA(Clause.of(Struct.of("f", Integer.of(1))))
    */
}