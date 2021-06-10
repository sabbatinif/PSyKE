package it.unibo.skpf.re

import it.unibo.skpf.re.OriginalValue.Interval
import it.unibo.skpf.re.OriginalValue.Value
import it.unibo.tuprolog.core.Atom
import it.unibo.tuprolog.core.List
import it.unibo.tuprolog.core.Real
import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.core.Var
import smile.data.DataFrame
import smile.data.inputs
import java.lang.IllegalStateException

internal fun createTerm(v: Var?, constraint: OriginalValue, positive: Boolean = true): Struct {
    if (v == null)
        throw IllegalStateException()
    val functor = createFunctor(constraint, positive)
    return when (constraint) {
        is Interval.LessThan -> Struct.of(functor, v, Real.of(constraint.value))
        is Interval.GreaterThan -> Struct.of(functor, v, Real.of(constraint.value))
        is Interval.Between -> Struct.of(functor, v, List.of(Real.of(constraint.lower), Real.of(constraint.upper)))
        is Value -> Struct.of(functor, v, Atom.of(constraint.value.toString()))
    }
}

internal fun createFunctor(constraint: OriginalValue, positive: Boolean): String {
    return when(constraint) {
        is Interval.LessThan -> if (positive) "=<" else ">"
        is Interval.GreaterThan -> if (positive) ">" else "=<"
        is Interval.Between -> if (positive) "in" else "not_in"
        is Value -> if (positive) "=" else "\\="
    }
}

internal fun createVariableList(featureSet: Collection<BooleanFeatureSet>, dataset: DataFrame? = null): Map<String, Var> {
    val values =
        if (featureSet.isNotEmpty())
            featureSet.map { it.name to Var.of(it.name) }
        else dataset?.inputs()?.names()?.map { it to Var.of(it) } ?: throw IllegalStateException()
    return mapOf(*values.toTypedArray())
}

internal fun createHead(functor: String, variables: Collection<Var>, outClass: String): Struct {
    return Struct.of(functor, variables.plus(Atom.of(outClass)))
}
