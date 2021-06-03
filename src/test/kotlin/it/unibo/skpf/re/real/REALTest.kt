package it.unibo.skpf.re.real

import it.unibo.skpf.re.BooleanFeatureSet
import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.loadFromFile
import it.unibo.tuprolog.core.*
import it.unibo.tuprolog.theory.MutableTheory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import smile.classification.KNN
import smile.data.DataFrame
import smile.data.Tuple
import kotlin.streams.toList

internal class REALTest {

    val knn = loadFromFile("irisKNN9.txt") as KNN<DoubleArray>
    val featureSets = loadFromFile("irisBoolFeatSet.txt") as Set<BooleanFeatureSet>
    val real = Extractor.ruleExtractionAsLearning(knn, featureSets)
    val train = loadFromFile("irisTrain50.txt") as DataFrame
    val theory = real.extract(train)

    @Test
    fun extract() {
        fun createHead(variables: Collection<Term>, outputClass: String) =
            Struct.of("concept", variables.plus(Atom.of(outputClass)))

        val variables = listOf("V1", "V2", "V3", "V4").map { Var.of(it) }
        val cond1 = Struct.of("in", variables[0], Real.of(5.39), Real.of(6.26))
        val cond2 = Struct.of("in", variables[2], Real.of(2.28), Real.of(4.87))
        val cond3 = Struct.of("in", variables[3], Real.of(0.65), Real.of(1.64))

        val expectedTheory = MutableTheory.of(
            Clause.of(
                createHead(variables, "Iris-setosa"),
                Struct.of("in", variables[3], Real.of(0.09), Real.of(0.65))
            ),
            Clause.of(
                createHead(variables, "Iris-versicolor"),
                cond2, cond3
            ),
            Clause.of(
                createHead(variables, "Iris-versicolor"),
                cond1, cond3
            ),
            Clause.of(
                createHead(variables, "Iris-versicolor"),
                cond1, cond2
            ),
            Clause.of(
                createHead(variables, "Iris-virginica"),
                Struct.of("in", variables[2], Real.of(4.87), Real.of(6.91))
            ),
            Clause.of(
                createHead(variables, "Iris-virginica"),
                Struct.of("in", variables[1], Real.of(2.87), Real.of(3.2)),
                Struct.of("in", variables[3], Real.of(1.64), Real.of(2.51))
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
        val predictions = real.predict(test)
        val expected = sequence {
            for (sample in test.stream().toList())
                if (sample.check("V4_0"))
                    yield(0)
                else if (
                    sample.check("V3_1") && sample.check("V4_1") ||
                    sample.check("V1_1") && sample.check("V4_1") ||
                    sample.check("V3_1") && sample.check("V1_1")
                )
                    yield(1)
                else if (sample.check("V3_2") || sample.check("V2_1") && sample.check("V4_2"))
                    yield(2)
                else
                    yield(-1)
        }
        assertEquals(expected.toList(), predictions.toList())
    }
}