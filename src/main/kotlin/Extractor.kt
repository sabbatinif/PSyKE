import it.unibo.tuprolog.theory.MutableTheory
import it.unibo.tuprolog.theory.Theory
import smile.classification.Classifier
import smile.data.DataFrame
import java.util.function.ToDoubleFunction

interface Extractor<T, F : ToDoubleFunction<T>> {
    val predictor: F

    val dataset: DataFrame

    val featureSet: Set<BooleanFeatureSet>

    fun extract(x: Array<T>): Theory

    fun predict(x: DataFrame): IntArray

    fun createTheory(): MutableTheory
}