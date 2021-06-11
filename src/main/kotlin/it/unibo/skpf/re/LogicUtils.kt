package it.unibo.skpf.re

import it.unibo.skpf.re.OriginalValue.Interval
import it.unibo.skpf.re.OriginalValue.Value
import it.unibo.tuprolog.core.*
import it.unibo.tuprolog.core.List
import it.unibo.tuprolog.core.operators.Operator
import it.unibo.tuprolog.core.operators.OperatorSet
import it.unibo.tuprolog.core.operators.Specifier
import smile.data.DataFrame
import smile.data.inputs
import java.lang.IllegalStateException

internal fun createTerm(v: Var?, constraint: OriginalValue, positive: Boolean = true): Struct {
    if (v == null)
        throw IllegalStateException()
    val functor = createFunctor(constraint, positive)
    return when (constraint) {
        is Interval.LessThan -> Struct.of(functor, v, Real.of(constraint.value.round(2)))
        is Interval.GreaterThan -> Struct.of(functor, v, Real.of(constraint.value.round(2)))
        is Interval.Between -> Struct.of(functor, v,
            List.of(Real.of(constraint.lower.round(2)), Real.of(constraint.upper.round(2))))
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

internal fun createHead(functor: String, variables: Collection<Var>, output: Number): Struct {
    val value = if (output is Double) output.round(2) else output
    return Struct.of(functor, variables.plus(Numeric.of(value)))
}

internal fun prettyRulesFormatter() =
    TermFormatter.prettyExpressions(
        OperatorSet.DEFAULT +
            Operator("in", Specifier.XFX, 800) +
            Operator("not_in", Specifier.XFX, 800)
    )