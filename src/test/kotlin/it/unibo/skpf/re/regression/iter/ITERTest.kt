package it.unibo.skpf.re.regression.iter

import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.utils.greaterOrEqualThan
import it.unibo.skpf.re.utils.lessThan
import it.unibo.skpf.re.utils.loadFromFile
import it.unibo.tuprolog.dsl.theory.prolog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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
                rule {
                    "z"(X, Y, 0.3) `if`
                        (structOf("in", X, listOf(0.5, 1.0)) and structOf("in", Y, listOf(0.0, 0.49)))
                },
                rule {
                    "z"(X, Y, 0.09) `if`
                        (structOf("in", X, listOf(0.5, 1.0)) and structOf("in", Y, listOf(0.49, 1.0)))
                },
                rule {
                    "z"(X, Y, 0.34) `if`
                        (structOf("in", X, listOf(0.0, 0.5)) and structOf("in", Y, listOf(0.49, 1.0)))
                },
                rule {
                    "z"(X, Y, 0.64) `if`
                        (structOf("in", X, listOf(0.0, 0.5)) and structOf("in", Y, listOf(0.0, 0.49)))
                }
            )
        }
        assertEquals(expected.size, theory.size)
        assertTrue(expected.equals(theory, useVarCompleteName = false))
    }

    @Test
    fun predict() {
        val test = (loadFromFile("artiTest50.txt") as DataFrame)
        val predictions = iter.predict(test).toList()
        val expected = sequence {
            for (sample in test.stream().toList()) {
                if (sample.greaterOrEqualThan("X", 0.495) &&
                    sample.lessThan("Y", 0.49)
                )
                    yield(0.2990948845503994)
                else if (sample.lessThan("X", 0.495) &&
                    sample.greaterOrEqualThan("Y", 0.49)
                )
                    yield(0.3412396997200023)
                else if (sample.greaterOrEqualThan("X", 0.495) &&
                    sample.greaterOrEqualThan("Y", 0.49)
                )
                    yield(0.09333036044583674)
                else if (sample.lessThan("X", 0.495) &&
                    sample.lessThan("Y", 0.49)
                )
                    yield(0.6354766542797601)
            }
        }.toList()
        assertEquals(expected, predictions)
    }
}
