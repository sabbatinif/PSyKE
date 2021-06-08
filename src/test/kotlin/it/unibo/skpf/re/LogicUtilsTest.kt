package it.unibo.skpf.re

import it.unibo.skpf.re.OriginalValue.Interval
import it.unibo.skpf.re.OriginalValue.Value
import it.unibo.tuprolog.core.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import smile.data.splitFeatures
import smile.io.Read
import java.util.stream.Stream

class LogicUtilsTest {
    @ParameterizedTest
    @ArgumentsSource(Companion::class)
    fun testCreateTerm(
        constraint: OriginalValue, positive: Boolean,
        functor: String, terms: Array<Term>
    ) {
        val expected = Struct.of(functor, Var.of("V"), *terms)
        val actual = createTerm(Var.of("V"), constraint, positive)
        assertTrue(expected.structurallyEquals(actual))
    }

    @Test
    fun testCreateVariableList() {
        val expected = mapOf(
            "V1" to Var.of("V1"),
            "V2" to Var.of("V2"),
            "V3" to Var.of("V3"),
            "V4" to Var.of("V4")
        )
        val actual = createVariableList(Read.csv("datasets/iris.data").splitFeatures())
        assertEquals(expected.keys, actual.keys)
        expected.values.zip(actual.values) { exp, act ->
            assert(exp.structurallyEquals(act))
        }
    }

    @Test
    fun testCreateHead() {
        val outputClass = "class"
        val vars = arrayOf(Var.of("v1"), Var.of("v2"), Var.of("v3"))
        val terms = listOf(*vars, Atom.of(outputClass))
        assertEquals(
            Struct.of("functor", terms),
            createHead("functor", vars.toList(), outputClass)
        )
    }

    companion object : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(
                Arguments.of(
                    Value("test"), true, "equal",
                    arrayOf(Atom.of("test"))
                ),
                Arguments.of(
                    Interval.Between(2.6, 8.9), true, "in",
                    arrayOf(Real.of(2.6), Real.of(8.9))
                ),
                Arguments.of(
                    Interval.LessThan(6.3), true, "le",
                    arrayOf(Real.of(6.3))
                ),
                Arguments.of(
                    Interval.GreaterThan(3.2), true, "gt",
                    arrayOf(Real.of(3.2))
                ),
                Arguments.of(
                    Value(2.65), false, "not_equal",
                    arrayOf(Atom.of("2.65"))
                ),
                Arguments.of(
                    Interval.Between(14.3, 25.2), false, "not_in",
                    arrayOf(Real.of(14.3), Real.of(25.2))
                ),
                Arguments.of(
                    Interval.LessThan(12.6), false, "not_le",
                    arrayOf(Real.of(12.6))
                ),
                Arguments.of(
                    Interval.GreaterThan(5.3), false, "not_gt",
                    arrayOf(Real.of(5.3))
                )
            )
        }
    }
}