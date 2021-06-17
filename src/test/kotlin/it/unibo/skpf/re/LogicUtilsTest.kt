package it.unibo.skpf.re

import it.unibo.skpf.re.schema.Value
import it.unibo.skpf.re.schema.Value.Constant
import it.unibo.skpf.re.schema.Value.Interval
import it.unibo.skpf.re.schema.Value.Interval.Between
import it.unibo.skpf.re.schema.Value.Interval.GreaterThan
import it.unibo.skpf.re.schema.Value.Interval.LessThan
import it.unibo.skpf.re.utils.createFunctor
import it.unibo.skpf.re.utils.createHead
import it.unibo.skpf.re.utils.createTerm
import it.unibo.skpf.re.utils.createVariableList
import it.unibo.tuprolog.core.Atom
import it.unibo.tuprolog.core.List
import it.unibo.tuprolog.core.Numeric
import it.unibo.tuprolog.core.Real
import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.core.Term
import it.unibo.tuprolog.core.Var
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
import java.lang.IllegalStateException
import java.util.stream.Stream

class LogicUtilsTest {
    @ParameterizedTest
    @ArgumentsSource(TermArguments::class)
    fun testCreateTerm(
        constraint: Value,
        positive: Boolean,
        functor: String,
        term: Term
    ) {
        val expected = Struct.of(functor, Var.of("V"), term)
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

    @ParameterizedTest
    @ArgumentsSource(FunctorArguments::class)
    fun testCreateFunctor(originalValue: Value, positive: Boolean, functor: String) {
        assertEquals(functor, createFunctor(originalValue, positive))
    }

    @ParameterizedTest
    @ArgumentsSource(HeadArguments::class)
    fun testCreateHead(output: Any) {
        val vars = arrayOf(Var.of("v1"), Var.of("v2"), Var.of("v3"))
        assertEquals(
            Struct.of(
                "functor",
                listOf(
                    *vars,
                    when (output) {
                        is Number -> Numeric.of(output)
                        is String -> Atom.of(output)
                        else -> throw IllegalStateException()
                    }
                )
            ),
            when (output) {
                is Number -> createHead("functor", vars.toList(), output)
                is String -> createHead("functor", vars.toList(), output)
                else -> throw IllegalStateException()
            }
        )
    }

    object HeadArguments : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(
                Arguments.of("setosa"),
                Arguments.of(1),
                Arguments.of(2.5)
            )
        }
    }

    object FunctorArguments : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(
                Arguments.of(LessThan(5.3), true, "=<"),
                Arguments.of(LessThan(4.6), false, ">"),
                Arguments.of(GreaterThan(9.6), true, ">"),
                Arguments.of(GreaterThan(3.2), false, "=<"),
                Arguments.of(Between(1.2, 3.6), true, "in"),
                Arguments.of(Between(2.6, 7.1), false, "not_in"),
                Arguments.of(Constant("V"), true, "="),
                Arguments.of(Constant("value"), false, "\\=")
            )
        }
    }

    object TermArguments : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(
                Arguments.of(
                    Constant("test"), true, "=", Atom.of("test")
                ),
                Arguments.of(
                    Interval.Between(2.6, 8.9), true, "in",
                    List.of(Real.of(2.6), Real.of(8.9))
                ),
                Arguments.of(
                    Interval.LessThan(6.3), true, "=<", Real.of(6.3)
                ),
                Arguments.of(
                    Interval.GreaterThan(3.2), true, ">", Real.of(3.2)
                ),
                Arguments.of(
                    Constant(2.65), false, "\\=", Atom.of("2.65")
                ),
                Arguments.of(
                    Interval.Between(14.3, 25.2), false, "not_in",
                    List.of(Real.of(14.3), Real.of(25.2))
                ),
                Arguments.of(
                    Interval.LessThan(12.6), false, ">", Real.of(12.6)
                ),
                Arguments.of(
                    Interval.GreaterThan(5.3), false, "=<", Real.of(5.3)
                )
            )
        }
    }
}
