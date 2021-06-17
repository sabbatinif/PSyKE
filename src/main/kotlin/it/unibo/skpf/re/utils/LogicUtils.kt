package it.unibo.skpf.re.utils

import it.unibo.skpf.re.schema.Feature
import it.unibo.skpf.re.schema.Value
import it.unibo.skpf.re.schema.Value.Interval.Between
import it.unibo.skpf.re.schema.Value.Interval.GreaterThan
import it.unibo.skpf.re.schema.Value.Interval.LessThan
import it.unibo.skpf.re.schema.Value.Constant
import it.unibo.skpf.re.utils.LogicUtils.priority
import it.unibo.tuprolog.core.Atom
import it.unibo.tuprolog.core.List
import it.unibo.tuprolog.core.Numeric
import it.unibo.tuprolog.core.Real
import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.core.TermFormatter
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.core.operators.Operator
import it.unibo.tuprolog.core.operators.OperatorSet
import it.unibo.tuprolog.core.operators.Specifier
import smile.data.DataFrame
import smile.data.inputs

object LogicUtils {
    const val priority = 800
}

internal fun createTerm(v: Var?, constraint: Value, positive: Boolean = true): Struct {
    if (v == null)
        throw IllegalArgumentException("Null variable")
    val functor = createFunctor(constraint, positive)
    return when (constraint) {
        is LessThan -> Struct.of(functor, v, Real.of(constraint.value.round(2)))
        is GreaterThan -> Struct.of(functor, v, Real.of(constraint.value.round(2)))
        is Between -> Struct.of(
            functor, v,
            List.of(Real.of(constraint.lower.round(2)), Real.of(constraint.upper.round(2)))
        )
        is Constant -> Struct.of(functor, v, Atom.of(constraint.value.toString()))
    }
}

internal fun createFunctor(constraint: Value, positive: Boolean): String {
    return when (constraint) {
        is LessThan -> if (positive) "=<" else ">"
        is GreaterThan -> if (positive) ">" else "=<"
        is Between -> if (positive) "in" else "not_in"
        is Constant -> if (positive) "=" else "\\="
    }
}

internal fun createVariableList(
    feature: Collection<Feature>,
    dataset: DataFrame? = null
): Map<String, Var> {
    val values =
        if (feature.isNotEmpty())
            feature.map { it.name to Var.of(it.name) }
        else
            dataset?.inputs()?.names()?.map { it to Var.of(it) }
                ?: throw NullPointerException("dataset cannot be null if featureSet is empty")
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
            Operator("in", Specifier.XFX, priority) +
            Operator("not_in", Specifier.XFX, priority)
    )
