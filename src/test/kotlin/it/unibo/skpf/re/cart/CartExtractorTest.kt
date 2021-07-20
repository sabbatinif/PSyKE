package it.unibo.skpf.re.cart

import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.Schemas
import it.unibo.skpf.re.cart.ExpectedValues.artiExpected
import it.unibo.skpf.re.cart.ExpectedValues.artiTest
import it.unibo.skpf.re.cart.ExpectedValues.expectedArtiTheory
import it.unibo.skpf.re.cart.ExpectedValues.expectedIrisTheory
import it.unibo.skpf.re.cart.ExpectedValues.irisExpected
import it.unibo.skpf.re.cart.ExpectedValues.irisTest
import it.unibo.skpf.re.utils.loadFromFile
import it.unibo.tuprolog.theory.Theory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import smile.base.cart.SplitRule
import smile.classification.cart
import smile.data.DataFrame
import smile.data.classes
import smile.data.formula.Formula
import smile.data.inputs
import smile.data.name
import smile.data.outputs
import java.util.stream.Stream

internal class CartExtractorTest {

    @ParameterizedTest
    @ArgumentsSource(ExtractArguments::class)
    fun testExtract(expectedTheory: Theory, extractor: CartExtractor, train: DataFrame) {
        val theory = extractor.extract(train)
        assertEquals(expectedTheory.size, theory.size)
        assertTrue(expectedTheory.equals(theory, useVarCompleteName = false))
    }

    @ParameterizedTest
    @ArgumentsSource(PredictArguments::class)
    fun testPredict(expected: List<*>, extractor: CartExtractor, test: DataFrame) {
        val predictions = extractor.predict(test)
        assertEquals(expected, predictions.toList())
    }

    object ArgumentUtils {
        fun loadDataFrame(path: String) = loadFromFile(path) as DataFrame

        fun classifierModel(train: DataFrame) = Extractor.cart(
            CartPredictor(
                cart(
                    Formula.lhs("Class"),
                    train.inputs().merge(train.classes()),
                    SplitRule.GINI, 20, 0, 5
                )
            ),
            Schemas.iris
        )

        fun regressionModel(train: DataFrame) = Extractor.cart(
            CartPredictor(
                smile.regression.cart(
                    Formula.lhs(train.name()),
                    train.inputs().merge(train.outputs()),
                    3, 0, 5
                )
            )
        )
    }

    object ExtractArguments : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            val irisTrain = ArgumentUtils.loadDataFrame("irisTrain50.txt")
            val artiTrain = ArgumentUtils.loadDataFrame("artiTrain50.txt")

            val irisCartEx = ArgumentUtils.classifierModel(irisTrain)
            val artiCartEx = ArgumentUtils.regressionModel(artiTrain)

            return Stream.of(
                Arguments.of(expectedIrisTheory, irisCartEx, irisTrain),
                Arguments.of(expectedArtiTheory, artiCartEx, artiTrain)
            )
        }
    }

    object PredictArguments : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            val irisTrain = ArgumentUtils.loadDataFrame("irisTrain50.txt")
            val artiTrain = ArgumentUtils.loadDataFrame("artiTrain50.txt")

            val irisCartEx = ArgumentUtils.classifierModel(irisTrain)
            val artiCartEx = ArgumentUtils.regressionModel(artiTrain)

            return Stream.of(
                Arguments.of(irisExpected.toList(), irisCartEx, irisTest),
                Arguments.of(artiExpected.toList(), artiCartEx, artiTest)
            )
        }
    }
}
