package it.unibo.skpf.re.cart

import it.unibo.skpf.re.OriginalValue
import it.unibo.skpf.re.getFieldValue
import smile.base.cart.OrdinalNode
import smile.data.type.StructType

typealias LeafConstraints = List<Pair<String, OriginalValue>>
typealias LeafSequence = Sequence<Pair<LeafConstraints, String>>

val OrdinalNode.value: Double
    get() = getFieldValue("value")

internal fun OrdinalNode.split(fields: StructType) = Pair(
    fields.field(this.feature()).name to OriginalValue.Interval.LessThan(this.value),
    fields.field(this.feature()).name to OriginalValue.Interval.GreaterThan(this.value)
)