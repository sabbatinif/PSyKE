package it.unibo.skpf.re

import it.unibo.skpf.re.cart.CartExtractor
import it.unibo.skpf.re.duepan.Duepan
import it.unibo.skpf.re.real.REAL
import it.unibo.tuprolog.theory.Theory
import smile.classification.Classifier
import smile.classification.DecisionTree
import smile.data.DataFrame
import smile.data.Tuple
import java.util.function.ToDoubleFunction

/**
 * An explanator capable of extracting rules from trained black box
 * @param T is a parameter for defining the underlying black box type
 * @param F is the type of the underlying black box
 */
interface Extractor<T, F : ToDoubleFunction<T>> {

    /**
     * The underlying black box predictor
     */
    val predictor: F

    /**
     * A collection of sets of discretised features. Each set corresponds
     * to a set of features derived from a single non-discrete feature
     */
    val featureSet: Collection<BooleanFeatureSet>

    /**
     * Extracts rules from the underlying predictor
     * @param dataset is the set of instances to be used for the extraction
     * @return the theory created from the extracted rules
     */
    fun extract(dataset: DataFrame): Theory

    /**
     * Predicts the output values of every sample in dataset
     * @param dataset is the set of instances to predict
     * @return an array of predictions
     */
    fun predict(dataset: DataFrame): Array<*>

    companion object {
        /**
         * Creates a new Rule-Extraction-As-Learning extractor
         */
        @JvmStatic
        fun ruleExtractionAsLearning(
            predictor: Classifier<DoubleArray>,
            featureSet: Collection<BooleanFeatureSet>
        ): Extractor<DoubleArray, Classifier<DoubleArray>> = REAL(predictor, featureSet)

        /**
         * Creates a new Duepan extractor
         */
        @JvmStatic
        fun duepan(
            predictor: Classifier<DoubleArray>,
            featureSet: Collection<BooleanFeatureSet>,
            minExamples: Int = 0
        ): Extractor<DoubleArray, Classifier<DoubleArray>> = Duepan(predictor, featureSet, minExamples)

        /**
         * Creates a new CART extractor
         */
        @JvmStatic
        fun cart(
            predictor: DecisionTree,
            featureSet: Collection<BooleanFeatureSet>,
        ): Extractor<Tuple, DecisionTree> = CartExtractor(predictor, featureSet)
    }
}