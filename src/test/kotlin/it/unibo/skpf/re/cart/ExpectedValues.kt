package it.unibo.skpf.re.cart

import it.unibo.skpf.re.utils.check
import it.unibo.skpf.re.utils.greaterThan
import it.unibo.skpf.re.utils.lessOrEqualThan
import it.unibo.tuprolog.dsl.theory.prolog
import kotlin.streams.toList

object ExpectedValues {
    val expectedArtiTheory = prolog {
        theoryOf(
            rule { "z"(X, Y, 0.7) `if` ((X lowerThanOrEqualsTo 0.5) and (Y lowerThanOrEqualsTo 0.5)) },
            rule { "z"(X, Y, 0.4) `if` ((X lowerThanOrEqualsTo 0.5) and (Y greaterThan 0.5)) },
            rule { "z"(X, Y, 0.3) `if` ((X greaterThan 0.5) and (Y lowerThanOrEqualsTo 0.5)) },
            rule { "z"(X, Y, 0.0) `if` ((X greaterThan 0.5) and (Y greaterThan 0.5)) },
        )
    }

    val expectedIrisTheory = prolog {
        theoryOf(
            rule {
                "iris"("SepalLength", "SepalWidth", "PetalLength", "PetalWidth", "versicolor") `if`
                    (("PetalLength" greaterThan 2.28) and ("PetalWidth" lowerThanOrEqualsTo 1.64))
            },
            rule {
                "iris"("SepalLength", "SepalWidth", "PetalLength", "PetalWidth", "virginica") `if`
                    (("PetalLength" greaterThan 2.28) and ("PetalWidth" greaterThan 1.64))
            },
            rule {
                "iris"("SepalLength", "SepalWidth", "PetalLength", "PetalWidth", "setosa") `if`
                    ("PetalLength" lowerThanOrEqualsTo 2.28)
            },
        )
    }

    val artiTest = CartExtractorTest.ArgumentUtils.loadDataFrame("artiTrain50.txt")
    val artiExpected = sequence {
        for (sample in artiTest.stream().toList()) {
            if (sample.lessOrEqualThan("X", 0.5) &&
                sample.lessOrEqualThan("Y", 0.5)
            )
                yield(0.7000000000000004)
            else if (sample.lessOrEqualThan("X", 0.5) &&
                sample.greaterThan("Y", 0.5)
            )
                yield(0.39999999999999986)
            else if (sample.greaterThan("X", 0.5) &&
                sample.greaterThan("Y", 0.5)
            )
                yield(0.0)
            else if (sample.greaterThan("X", 0.5) &&
                sample.lessOrEqualThan("Y", 0.5)
            )
                yield(0.29999999999999893)
        }
    }

    val irisTest = CartExtractorTest.ArgumentUtils.loadDataFrame("irisTest50.txt")
    val irisExpected = sequence {
        for (sample in irisTest.stream().toList()) {
            if (!sample.check("PetalLength_0")) {
                if (!sample.check("PetalWidth_1"))
                    yield(2)
                else
                    yield(1)
            } else
                yield(0)
        }
    }
}
