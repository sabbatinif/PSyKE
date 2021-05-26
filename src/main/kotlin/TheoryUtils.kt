import it.unibo.tuprolog.core.*
import OriginalValue.Interval
import OriginalValue.Value

fun createTerm(v: Var?, constraint: OriginalValue?, positive: Boolean = true): Struct {
    val functor = (if (!positive) "not_" else "") +
            (if (constraint is Interval) "in" else "equal")
    if (v == null)
        return Struct.of(functor)
    return when (constraint) {
        is Interval -> Struct.of(functor, v, Real.of(constraint.lower), Real.of(constraint.upper))
        is Value -> Struct.of(functor, v, Atom.of(constraint.value.toString()))
        else -> Struct.of(functor)
    }
}

fun createVariableList(featureSet: Set<BooleanFeatureSet>): Map<String, Var> {
    return mapOf(*featureSet.map {
        it.name to Var.of(it.name)
    }.toTypedArray())
}

fun createHead(functor: String, variables: Collection<Var>, outClass: String): Struct {
    return Struct.of(functor, variables.plus(Atom.of(outClass)))
}