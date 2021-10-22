package it.unibo.skpf.re.regression.iter

import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.utils.greaterOrEqualThan
import it.unibo.skpf.re.utils.greaterThan
import it.unibo.skpf.re.utils.lessThan
import it.unibo.skpf.re.utils.loadFromFile
import it.unibo.tuprolog.dsl.theory.prolog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import smile.data.DataFrame
import smile.regression.RBFNetwork
import kotlin.streams.toList

internal class GridExTest {

    private val rbf = loadFromFile("artiRBF95.txt") as RBFNetwork<DoubleArray>
    private val gridex = Extractor.gridex(rbf, threshold = 0.1, minExamples = 15, steps = listOf(2, 2))
    private val train = loadFromFile("artiTrain50.txt") as DataFrame
    private val theory = gridex.extract(train)

    @Test
    fun extract() {
        val expected = prolog {
            theoryOf(
                rule {
                    "z"(X, Y, 0.69) `if`
                        (structOf("in", X, listOf(0.0, 0.5)) and structOf("in", Y, listOf(0.0, 0.5)))
                },
                rule {
                    "z"(X, Y, 0.3) `if`
                        (structOf("in", X, listOf(0.5, 1.0)) and structOf("in", Y, listOf(0.0, 0.5)))
                },
                rule {
                    "z"(X, Y, 0.4) `if`
                        (structOf("in", X, listOf(0.0, 0.5)) and structOf("in", Y, listOf(0.5, 1.0)))
                },
                rule {
                    "z"(X, Y, 0.01) `if`
                        (structOf("in", X, listOf(0.5, 1.0)) and structOf("in", Y, listOf(0.5, 1.0)))
                }
            )
        }
        assertEquals(expected.size, theory.size)
        assertTrue(expected.equals(theory, useVarCompleteName = false))
    }

    @Test
    fun predict() {
        val test = (loadFromFile("artiTest50.txt") as DataFrame)
        val predictions = gridex.predict(test).toList()
        val expected = sequence {
            for (sample in test.stream().toList()) {
                if (sample.lessThan("X", 0.5) &&
                    sample.lessThan("Y", 0.5)
                )
                    yield(0.6893487778909353)
                else if (sample.greaterOrEqualThan("X", 0.5) &&
                    sample.lessThan("Y", 0.5)
                )
                    yield(0.30061006043251626)
                else if (sample.lessThan("X", 0.5) &&
                    sample.greaterOrEqualThan("Y", 0.5)
                )
                    yield(0.3988985786184592)
                else if (sample.greaterThan("X", 0.495) &&
                    sample.greaterThan("Y", 0.49)
                )
                    yield(0.012062604720732905)
            }
        }.toList()
        assertEquals(expected, predictions)
    }
}
