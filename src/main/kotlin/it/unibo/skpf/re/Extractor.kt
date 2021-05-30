package it.unibo.skpf.re

import it.unibo.skpf.re.duepan.Duepan
import it.unibo.skpf.re.real.REAL
import it.unibo.tuprolog.theory.Theory
import smile.classification.Classifier
import smile.data.DataFrame
import java.io.Serializable
import java.util.function.ToDoubleFunction

interface Extractor<T, F : ToDoubleFunction<T>>: Serializable {
    val predictor: F

    val featureSet: Set<BooleanFeatureSet>

    fun extract(dataset: DataFrame): Theory

    fun predict(dataset: DataFrame): IntArray

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