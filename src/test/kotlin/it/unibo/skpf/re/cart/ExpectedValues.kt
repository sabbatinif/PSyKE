package it.unibo.skpf.re.cart

import it.unibo.skpf.re.utils.createHead
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Real
import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.dsl.theory.prolog
import it.unibo.tuprolog.theory.Theory

object ExpectedValues {

    val expectedArtiTheory = prolog {
        theoryOf(
            rule { "z"(X, Y, 0.7) `if` ((X lowerThanOrEqualsTo 0.5) and (Y lowerThanOrEqualsTo 0.5)) },
            rule { "z"(X, Y, 0.4) `if` ((X lowerThanOrEqualsTo 0.5) and (Y greaterThan 0.5)) },
            rule { "z"(X, Y, 0.3) `if` ((X greaterThan 0.5) and (Y lowerThanOrEqualsTo 0.5)) },
            rule { "z"(X, Y, 0.0) `if` ((X greaterThan 0.5) and (Y greaterThan  0.5)) },
        )
    }

    private val irisVariables = listOf("SepalLength", "SepalWidth", "PetalLength", "PetalWidth").map { Var.of(it) }

    val expectedIrisTheory = Theory.of(
        Clause.of(
            createHead("iris", irisVariables, "versicolor"),
            Struct.of(">", irisVariables[2], Real.of(2.28)),
            Struct.of("=<", irisVariables[3], Real.of(1.64))
        ),
        Clause.of(
            createHead("iris", irisVariables, "virginica"),
            Struct.of(">", irisVariables[2], Real.of(2.28)),
            Struct.of(">", irisVariables[3], Real.of(1.64))
        ),
        Clause.of(
            createHead("iris", irisVariables, "setosa"),
            Struct.of("=<", irisVariables[2], Real.of(2.28))
        )
    )
}
