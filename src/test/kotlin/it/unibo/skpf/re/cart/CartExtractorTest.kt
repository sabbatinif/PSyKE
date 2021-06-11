package it.unibo.skpf.re.cart

import it.unibo.skpf.re.*
import it.unibo.tuprolog.core.*
import it.unibo.tuprolog.theory.MutableTheory
import it.unibo.tuprolog.theory.Theory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import smile.base.cart.SplitRule
import smile.classification.cart
import smile.data.*
import smile.data.formula.Formula
import java.util.stream.Stream

internal class CartExtractorTest {

    @ParameterizedTest
    @ArgumentsSource(Companion::class)
    fun extract(expectedTheory: Theory, extractor: CartExtractor, train: DataFrame) {
        val theory = extractor.extract(train)
        assertEquals(expectedTheory.size, theory.size)
        expectedTheory.zip(theory) { expected, actual ->
            assertTrue(expected.structurallyEquals(actual))
        }
    }

    companion object : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            @Suppress("UNCHECKED_CAST")
            val irisTrain = loadFromFile("irisTrain50.txt") as DataFrame
            val artiTrain = loadFromFile("artiTrain50.txt") as DataFrame
            val irisCart = cart(
                Formula.lhs("Class"),
                irisTrain.inputs().merge(irisTrain.classes()),
                SplitRule.GINI,20, 0, 5
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

            val irisVariables = listOf("SepalLength", "SepalWidth", "PetalLength", "PetalWidth").map { Var.of(it) }
            val expectedIrisTheory = MutableTheory.of(
                Clause.of(
                    createHead("iris", irisVariables, "versicolor"),
                    Struct.of(">", irisVariables[2], Real.of(2.28)),
                    Struct.of("=<", irisVariables[3], Real.of(1.64))
                ),
                Clause.of(
                    createHead("iris", irisVariables, "virginica"),
                    Struct.of(">", irisVariables[2], Real.of(2.28)),
                    Struct.of(">", irisVariables[3], Real.of(1.64))
                ),
                Clause.of(
                    createHead("iris", irisVariables, "setosa"),
                    Struct.of("=<", irisVariables[2], Real.of(2.28))
                )
            )

            val artiVariables = listOf("X", "Y").map { Var.of(it) }
            val expectedArtiTheory = MutableTheory.of(
                Clause.of(
                    createHead("z", artiVariables, 0.7),
                    Struct.of("=<", artiVariables[0], Real.of(0.5)),
                    Struct.of("=<", artiVariables[1], Real.of(0.5))
                ),
                Clause.of(
                    createHead("z", artiVariables, 0.4),
                    Struct.of("=<", artiVariables[0], Real.of(0.5)),
                    Struct.of(">", artiVariables[1], Real.of(0.5))
                ),
                Clause.of(
                    createHead("z", artiVariables, 0.3),
                    Struct.of(">", artiVariables[0], Real.of(0.5)),
                    Struct.of("=<", artiVariables[1], Real.of(0.5))
                ),
                Clause.of(
                    createHead("z", artiVariables, 0.0),
                    Struct.of(">", artiVariables[0], Real.of(0.5)),
                    Struct.of(">", artiVariables[1], Real.of(0.5))
                )
            )

            return Stream.of(
                Arguments.of(
                    expectedIrisTheory,
                    irisCartEx,
                    irisTrain
                ),
                Arguments.of(
                    expectedArtiTheory,
                    artiCartEx,
                    artiTrain
                )
            )
        }
    }
}