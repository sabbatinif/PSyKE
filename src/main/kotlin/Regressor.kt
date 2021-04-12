import smile.base.mlp.LayerBuilder
import smile.math.TimeFunction
import smile.regression.MLP

fun regressor(
    x: Array<DoubleArray>, y: DoubleArray,builders: Array<LayerBuilder>, epochs: Int = 10,
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