package it.unibo.skpf.re.duepan

import it.unibo.skpf.re.BooleanFeatureSet
import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.createHead
import it.unibo.skpf.re.loadFromFile
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Real
import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.theory.MutableTheory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import smile.classification.KNN
import smile.data.DataFrame
import smile.data.Tuple
import kotlin.streams.toList

internal class DuepanTest {

    private val knn = loadFromFile("irisKNN9.txt") as KNN<DoubleArray>
    private val featureSets = loadFromFile("irisBoolFeatSet.txt") as Set<BooleanFeatureSet>
    private val duepan = Extractor.duepan(knn, featureSets)
    private val train = loadFromFile("irisTrain50.txt") as DataFrame
    private val theory = duepan.extract(train)

    @Test
    fun extract() {
        val variables = listOf("V1", "V2", "V3", "V4").map { Var.of(it) }
        val expectedTheory = MutableTheory.of(
            Clause.of(
                createHead("concept", variables, "Iris-setosa"),
                Struct.of("in", variables[2], Real.of(0.99), Real.of(2.28))
            ),
            Clause.of(
                createHead("concept", variables, "Iris-virginica"),
                Struct.of("not_in", variables[2], Real.of(0.99), Real.of(2.28)),
                Struct.of("not_in", variables[3], Real.of(0.65), Real.of(1.64))
            ),
            Clause.of(
                createHead("concept", variables, "Iris-versicolor"),
                Struct.of("true")
            )
        )
        assertEquals(expectedTheory.size, theory.size)
        expectedTheory.zip(theory) { expected, actual ->
            assertTrue(expected.structurallyEquals(actual))
        }
    }

    @Test
    fun predict() {
        fun Tuple.check(field: String): Boolean =
            this[field].toString().toDouble() == 1.0

        val test = loadFromFile("irisTest50.txt") as DataFrame
        val predictions = duepan.predict(test)
        val expected = sequence {
            for (sample in test.stream().toList())
                if (sample.check("V3_0"))
                    yield(0)
                else if (!sample.check("V4_1"))
                    yield(2)
                else
                    yield(1)
        }
        assertEquals(expected.toList(), predictions.toList())
    }
}