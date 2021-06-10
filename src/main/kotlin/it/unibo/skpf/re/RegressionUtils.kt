package it.unibo.skpf.re

import smile.base.mlp.Layer
import smile.base.mlp.LayerBuilder
import smile.base.mlp.OutputFunction
import smile.data.*
import smile.io.Read
import smile.math.TimeFunction
import smile.regression.MLP
import smile.regression.rbfnet
import smile.validation.metric.*

fun regression(name: String, testSplit: Double) {
    println("*** $name ***")
    val dataset = Read.csv("datasets/$name")
    val (train, test) = dataset.randomSplit(testSplit)
    val x = train.inputsArray()
    val y = train.outputsArray()
    val mlp = MLPRegressor(
        x, y,
        arrayOf(Layer.tanh(35), Layer.tanh(15), Layer.mse(1, OutputFunction.LINEAR)),
        200,
        TimeFunction.linear(0.1, 10000.0, 0.05)
    )
    val rbf = rbfnet(x, y, 94, true)
    val pred = rbf.predict(test.inputsArray())
    println(mlp.metric(test.inputsArray(), test.outputsArray()).RSquared)
    println(rbf.metric(test.inputsArray(), test.outputsArray()).RSquared)
//    val mse = MSE.of(test.classesAsDoubleArray(), pred)
//    val r2 = R2.of(test.classesAsDoubleArray(), pred)
//    println(mse)
//    println(r2)

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
