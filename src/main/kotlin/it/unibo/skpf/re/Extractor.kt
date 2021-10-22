package it.unibo.skpf.re

import it.unibo.skpf.re.cart.CartExtractor
import it.unibo.skpf.re.cart.CartPredictor
import it.unibo.skpf.re.classification.real.REAL
import it.unibo.skpf.re.classification.trepan.SplitLogic
import it.unibo.skpf.re.classification.trepan.Trepan
import it.unibo.skpf.re.regression.gridex.GridEx
import it.unibo.skpf.re.regression.iter.ITER
import it.unibo.skpf.re.schema.Discretization
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
    val discretization: Discretization

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
            discretization: Discretization = Discretization.Empty,
        ): Extractor<DoubleArray, Classifier<DoubleArray>> = REAL(predictor, discretization)

        /**
         * Creates a new Trepan extractor.
         */
        @JvmStatic
        fun trepan(
            predictor: Classifier<DoubleArray>,
            discretization: Discretization = Discretization.Empty,
            minExamples: Int = 0,
            maxDepth: Int = 0,
            splitLogic: SplitLogic = SplitLogic.DEFAULT
        ): Extractor<DoubleArray, Classifier<DoubleArray>> =
            Trepan(predictor, discretization, minExamples, maxDepth, splitLogic)

        /**
         * Creates a new CART extractor.
         */
        @JvmStatic
        fun cart(
            predictor: CartPredictor,
            discretization: Discretization = Discretization.Empty,
        ): Extractor<Tuple, CartPredictor> = CartExtractor(predictor, discretization)

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
            ITER(
                predictor,
                Discretization.Empty,
                minUpdate,
                nPoints,
                maxIterations,
                minExamples,
                threshold,
                fillGaps
            )

        /**
         * Creates a new GridEx extractor.
         */
        @JvmStatic
        fun gridex(
            predictor: Regression<DoubleArray>,
            steps: Collection<Int>,
            threshold: Double,
            minExamples: Int
        ): Extractor<DoubleArray, Regression<DoubleArray>> =
            GridEx(
                predictor,
                Discretization.Empty,
                steps,
                threshold,
                minExamples
            )
    }
}
