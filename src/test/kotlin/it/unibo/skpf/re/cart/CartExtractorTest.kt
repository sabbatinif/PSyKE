package it.unibo.skpf.re.cart

import it.unibo.skpf.re.BooleanFeatureSet
import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.createHead
import it.unibo.skpf.re.loadFromFile
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Real
import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.theory.MutableTheory
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import smile.base.cart.SplitRule
import smile.classification.KNN
import smile.classification.cart
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.classes
import smile.data.formula.Formula
import smile.data.inputs
import kotlin.streams.toList

internal class CartExtractorTest {

    @Suppress("UNCHECKED_CAST")
    private val knn = loadFromFile("irisKNN9.txt") as KNN<DoubleArray>
    @Suppress("UNCHECKED_CAST")
    private val featureSets = loadFromFile("irisBoolFeatSet.txt") as Set<BooleanFeatureSet>
    private val train = loadFromFile("irisTrain50.txt") as DataFrame
    private val cart = cart(
        Formula.lhs("Class"),
        train.inputs().merge(train.classes()),
        SplitRule.GINI,
        20, 0, 5
    )
    private val cartEx = Extractor.cart(cart, featureSets)
    private val theory = cartEx.extract(train)

    @Test
    fun extract() {
        val variables = listOf("V1", "V2", "V3", "V4").map { Var.of(it) }
        val expectedTheory = MutableTheory.of(
            Clause.of(
                createHead("concept", variables, "Iris-versicolor"),
                Struct.of("le", variables[2], Real.of(0.5)),
                Struct.of("le", variables[3], Real.of(0.5))
            ),
            Clause.of(
                createHead("concept", variables, "Iris-virginica"),
                Struct.of("le", variables[2], Real.of(0.5)),
                Struct.of("gt", variables[3], Real.of(0.5))
            ),
            Clause.of(
                createHead("concept", variables, "Iris-setosa"),
                Struct.of("gt", variables[2], Real.of(0.5))
            )
        )
        assertEquals(expectedTheory.size, theory.size)
        expectedTheory.zip(theory) { expected, actual ->
            assertTrue(expected.structurallyEquals(actual))
        }
    }
}