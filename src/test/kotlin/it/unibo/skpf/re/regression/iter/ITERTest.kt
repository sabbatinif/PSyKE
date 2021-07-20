package it.unibo.skpf.re.regression.iter

import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.utils.greaterThan
import it.unibo.skpf.re.utils.lessOrEqualThan
import it.unibo.skpf.re.utils.loadFromFile
import it.unibo.tuprolog.dsl.theory.prolog
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import smile.data.DataFrame
import smile.regression.RBFNetwork
import kotlin.streams.toList

internal class ITERTest {

    private val rbf = loadFromFile("artiRBF95.txt") as RBFNetwork<DoubleArray>
    private val iter = Extractor.iter(rbf, minUpdate = 1.0 / 20, threshold = 0.19)
    private val train = loadFromFile("artiTrain50.txt") as DataFrame
    private val theory = iter.extract(train)

    @Test
    fun extract() {
        val expected = prolog {
            theoryOf(
                rule { "z"(X, Y, 0.3)
                    `if` (structOf("in", X, listOf(0.5, 1.0)) and structOf("in", Y, listOf(0.0, 0.49))) },
                rule { "z"(X, Y, 0.09)
                    `if` (structOf("in", X, listOf(0.5, 1.0)) and structOf("in", Y, listOf(0.49, 1.0))) },
                rule { "z"(X, Y, 0.34)
                    `if` (structOf("in", X, listOf(0.0, 0.5)) and structOf("in", Y, listOf(0.49, 1.0))) },
                rule { "z"(X, Y, 0.63)
                    `if` (structOf("in", X, listOf(0.0, 0.5)) and structOf("in", Y, listOf(0.0, 0.49))) }
            )
        }
        assertEquals(expected.size, theory.size)
        assertTrue(expected.equals(theory, useVarCompleteName = false))
    }

    @Test
    fun predict() {
        val test = loadFromFile("artiTest50.txt") as DataFrame
        val predictions = iter.predict(test).toList()
        val expected = sequence {
            for (sample in test.stream().toList()) {
                if (sample.greaterThan("X", 0.5) &&
                    sample.lessOrEqualThan("Y", 0.49))
                        yield(0.3)
                else if (sample.lessOrEqualThan("X", 0.5) &&
                    sample.greaterThan("Y", 0.49))
                    yield(0.34)
                else if (sample.greaterThan("X", 0.5) &&
                    sample.greaterThan("Y", 0.49))
                    yield(0.1)
                else if (sample.lessOrEqualThan("X", 0.5) &&
                    sample.lessOrEqualThan("Y", 0.49))
                    yield(0.63)
            }
        }.toList()
        assertEquals(expected, predictions)
    }
}
