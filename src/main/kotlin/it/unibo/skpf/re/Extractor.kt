package it.unibo.skpf.re

import it.unibo.skpf.re.cart.CartExtractor
import it.unibo.skpf.re.cart.CartPredictor
import it.unibo.skpf.re.classification.duepan.Duepan
import it.unibo.skpf.re.classification.real.REAL
import it.unibo.skpf.re.regression.iter.ITER
import it.unibo.skpf.re.schema.Schema
import it.unibo.tuprolog.theory.Theory
import smile.classification.Classifier
import smile.data.DataFrame
import smile.data.Tuple
import smile.regression.Regression
import java.util.function.ToDoubleFunction

/**
 * An explanator capable of extracting rules from trained black box.
 * @param T is a parameter for defining the underlying black box type.
 * @param F is the type of the underlying black box.
 */
interface Extractor<T, F : ToDoubleFunction<T>> {

    /**
     * The underlying black box predictor.
     */
    val predictor: F

    /**
     * A collection of sets of discretised features. Each set corresponds
     * to a set of features derived from a single non-discrete feature.
     */
    val schema: Schema

    /**
     * Extracts rules from the underlying predictor.
     * @param dataset is the set of instances to be used for the extraction.
     * @return the theory created from the extracted rules.
     */
    fun extract(dataset: DataFrame): Theory

    /**
     * Predicts the output values of every sample in dataset.
     * @param dataset is the set of instances to predict.
     * @return an array of predictions.
     */
    fun predict(dataset: DataFrame): Array<*>

    companion object {
        /**
         * Creates a new Rule-Extraction-As-Learning extractor.
         */
        @JvmStatic
        fun ruleExtractionAsLearning(
            predictor: Classifier<DoubleArray>,
            schema: Schema = Schema.Empty,
        ): Extractor<DoubleArray, Classifier<DoubleArray>> = REAL(predictor, schema)

        /**
         * Creates a new Duepan extractor.
         */
        @JvmStatic
        fun duepan(
            predictor: Classifier<DoubleArray>,
            schema: Schema = Schema.Empty,
            minExamples: Int = 0
        ): Extractor<DoubleArray, Classifier<DoubleArray>> = Duepan(predictor, schema, minExamples)

        /**
         * Creates a new CART extractor.
         */
        @JvmStatic
        fun cart(
            predictor: CartPredictor,
            schema: Schema = Schema.Empty,
        ): Extractor<Tuple, CartPredictor> = CartExtractor(predictor, schema)

        /**
         * Creates a new ITER extractor.
         */
        @JvmStatic
        fun iter(
            predictor: Regression<DoubleArray>,
            minUpdate: Double = 0.1,
            nPoints: Int = 1,
            maxIterations: Int = 600,
            minExamples: Int = 250,
            threshold: Double = 0.1,
            fillGaps: Boolean = false
        ): Extractor<DoubleArray, Regression<DoubleArray>> =
            ITER(predictor, Schema.Empty, minUpdate, nPoints, maxIterations, minExamples, threshold, fillGaps)
    }
}
