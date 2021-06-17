package it.unibo.skpf.re.cart

import it.unibo.skpf.re.BooleanFeatureSet
import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.cart.ExpectedValues.expectedArtiTheory
import it.unibo.skpf.re.cart.ExpectedValues.expectedIrisTheory
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
    @ArgumentsSource(Companion::class)
    fun testExtract(expectedTheory: Theory, extractor: CartExtractor, train: DataFrame) {
        val theory = extractor.extract(train)
        assertEquals(expectedTheory.size, theory.size)
        assertTrue(expectedTheory.equals(theory, useVarCompleteName = false))
    }

    companion object : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            val irisTrain = loadFromFile("irisTrain50.txt") as DataFrame
            val artiTrain = loadFromFile("artiTrain50.txt") as DataFrame
            val irisCart = cart(
                Formula.lhs("Class"),
                irisTrain.inputs().merge(irisTrain.classes()),
                SplitRule.GINI, 20, 0, 5
            )
            val artiCart = CartPredictor(
                smile.regression.cart(
                    Formula.lhs(artiTrain.name()),
                    artiTrain.inputs().merge(artiTrain.outputs()),
                    3, 0, 5
                )
            )
            @Suppress("UNCHECKED_CAST")
            val irisCartEx = Extractor.cart(
                CartPredictor(irisCart),
                loadFromFile("irisBoolFeatSet.txt") as Set<BooleanFeatureSet>
            )
            val artiCartEx = Extractor.cart(artiCart)

            return Stream.of(
                Arguments.of(expectedIrisTheory, irisCartEx, irisTrain),
                Arguments.of(expectedArtiTheory, artiCartEx, artiTrain)
            )
        }
    }
}
