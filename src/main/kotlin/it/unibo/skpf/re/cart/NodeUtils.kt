package it.unibo.skpf.re.cart

import it.unibo.skpf.re.getFieldValue
import smile.base.cart.OrdinalNode
import smile.data.type.StructType

val OrdinalNode.value: Double
    get() = getFieldValue("value")

data class NodeSplit(
    val feature: String,
    val value: Double,
    var positive: Boolean = true
) {
    override fun toString() =
        feature + (if (positive) " <= " else " > ") + value

    fun not(): NodeSplit {
        return this.copy(positive = false)
    }
}

internal fun OrdinalNode.split(fields: StructType): NodeSplit =
    NodeSplit(fields.field(this.feature()).name, this.value)