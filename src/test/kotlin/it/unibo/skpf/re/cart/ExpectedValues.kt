package it.unibo.skpf.re.cart

import it.unibo.skpf.re.utils.createHead
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Real
import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.theory.MutableTheory
import it.unibo.tuprolog.theory.Theory

object ExpectedValues {
    private val artiVariables = listOf("X", "Y").map { Var.of(it) }

    val expectedArtiTheory = Theory.of(
        Clause.of(
            createHead("z", artiVariables, 0.7),
            Struct.of("=<", artiVariables[0], Real.of(0.5)),
            Struct.of("=<", artiVariables[1], Real.of(0.5))
        ),
        Clause.of(
            createHead("z", artiVariables, 0.4),
            Struct.of("=<", artiVariables[0], Real.of(0.5)),
            Struct.of(">", artiVariables[1], Real.of(0.5))
        ),
        Clause.of(
            createHead("z", artiVariables, 0.3),
            Struct.of(">", artiVariables[0], Real.of(0.5)),
            Struct.of("=<", artiVariables[1], Real.of(0.5))
        ),
        Clause.of(
            createHead("z", artiVariables, 0.0),
            Struct.of(">", artiVariables[0], Real.of(0.5)),
            Struct.of(">", artiVariables[1], Real.of(0.5))
        )
    )

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
