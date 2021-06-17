package it.unibo.skpf.re.classification.duepan

import it.unibo.skpf.re.schema.Feature
import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.schema.Schema
import it.unibo.skpf.re.utils.createHead
import it.unibo.skpf.re.utils.loadFromFile
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.List
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

    @Suppress("UNCHECKED_CAST")
    private val knn = loadFromFile("irisKNN9.txt") as KNN<DoubleArray>
    @Suppress("UNCHECKED_CAST")
    private val featureSets: Schema = Schema.Unordered(loadFromFile("irisBoolFeatSet.txt"))
    private val duepan = Extractor.duepan(knn, featureSets)
    private val train = loadFromFile("irisTrain50.txt") as DataFrame
    private val theory = duepan.extract(train)

    @Test
    fun testExtract() {
        val variables = listOf("V1", "V2", "V3", "V4").map { Var.of(it) }
        val expectedTheory = MutableTheory.of(
            Clause.of(
                createHead("iris", variables, "setosa"),
                Struct.of("=<", variables[2], Real.of(2.28))
            ),
            Clause.of(
                createHead("iris", variables, "virginica"),
                Struct.of(">", variables[2], Real.of(2.28)),
                Struct.of("not_in", variables[3], List.of(Real.of(0.65), Real.of(1.64)))
            ),
            Clause.of(
                createHead("iris", variables, "versicolor"),
                Struct.of("true")
            )
        )
        assertEquals(expectedTheory.size, theory.size)
        expectedTheory.zip(theory) { expected, actual ->
            assertTrue(expected.structurallyEquals(actual))
        }
    }

    @Test
    fun testPredict() {
        fun Tuple.check(field: String): Boolean =
            this[field].toString().toDouble() == 1.0

        val test = loadFromFile("irisTest50.txt") as DataFrame
        val predictions = duepan.predict(test)
        val expected = sequence {
            for (sample in test.stream().toList())
                if (sample.check("PetalLength_0"))
                    yield(0)
                else if (!sample.check("PetalWidth_1"))
                    yield(2)
                else
                    yield(1)
        }
        assertEquals(expected.toList(), predictions.toList())
    }
}
