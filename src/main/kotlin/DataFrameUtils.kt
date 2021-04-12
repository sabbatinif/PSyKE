import smile.data.DataFrame
import smile.feature.Normalizer
import kotlin.random.Random

fun DataFrame.randomSplit(percent: Double, seed: Int = 0): Pair<DataFrame, DataFrame> {
    val r1 = Random(seed)
    val r2 = Random(seed)
    val test = DataFrame.of(stream().filter { r1.nextDouble() < percent })
    val train = DataFrame.of(stream().filter { r2.nextDouble() >= percent })
    return train to test
}

fun DataFrame.normalize(): DataFrame {
    val n = Normalizer(Normalizer.Norm.L2)
    return n.transform(this)
}