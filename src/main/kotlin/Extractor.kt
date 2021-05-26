import it.unibo.tuprolog.theory.Theory
import smile.classification.Classifier
import smile.data.DataFrame
import java.util.function.ToDoubleFunction

interface Extractor<T, F : ToDoubleFunction<T>> {
    val predictor: F

    val featureSet: Set<BooleanFeatureSet>

    fun extract(x: DataFrame): Theory

    fun predict(x: DataFrame): IntArray

    companion object {
        @JvmStatic
        fun ruleExtractionAsLearning(
            predictor: Classifier<DoubleArray>,
            featureSet: Set<BooleanFeatureSet>
        ): Extractor<DoubleArray, Classifier<DoubleArray>> = REAL(predictor, featureSet)

        @JvmStatic
        fun duepan(
            predictor: Classifier<DoubleArray>,
            featureSet: Set<BooleanFeatureSet>,
            minExamples: Int = 0
        ): Extractor<DoubleArray, Classifier<DoubleArray>> = Duepan(predictor, featureSet, minExamples)
    }
}