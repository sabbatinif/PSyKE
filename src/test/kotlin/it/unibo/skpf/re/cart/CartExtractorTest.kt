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
import smile.classification.cart
import smile.data.DataFrame
import smile.data.classes
import smile.data.formula.Formula
import smile.data.inputs

internal class CartExtractorTest {

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
        val variables = listOf("SepalLength", "SepalWidth", "PetalLength", "PetalWidth").map { Var.of(it) }
        val expectedTheory = MutableTheory.of(
            Clause.of(
                createHead("iris", variables, "versicolor"),
                Struct.of(">", variables[2], Real.of(2.28)),
                Struct.of("=<", variables[3], Real.of(1.64))
            ),
            Clause.of(
                createHead("iris", variables, "virginica"),
                Struct.of(">", variables[2], Real.of(2.28)),
                Struct.of(">", variables[3], Real.of(1.64))
            ),
            Clause.of(
                createHead("iris", variables, "setosa"),
                Struct.of("=<", variables[2], Real.of(2.28))
            )
        )
        assertEquals(expectedTheory.size, theory.size)
        expectedTheory.zip(theory) { expected, actual ->
            assertTrue(expected.structurallyEquals(actual))
        }
    }
}