package it.unibo.skpf.re.classification.trepan

import smile.data.DataFrame
import smile.data.nCategories
import smile.data.outputClasses

internal class Node(
    val samples: DataFrame,
    val nExamples: Int,
    var constraints: Set<Pair<String, Double>> = emptySet(),
    var children: MutableList<Node> = mutableListOf()
) {
    val priority: Double
        get() = -(this.reach * (1 - this.fidelity))

    val fidelity: Double
        get() = 1.0 * this.correct / this.samples.nrows()

    val reach: Double
        get() = 1.0 * this.samples.nrows() / this.nExamples

    val correct: Int
        get() = this.samples.outputClasses().count { it == this.dominant }

    val dominant: Any
        get() = this.samples.outputClasses()
            .groupBy { it }
            .mapValues { it.value.size }
            .maxByOrNull { it.value }?.key ?: ""

    val nClasses: Int
        get() = this.samples.nCategories()

    val asSequence: Sequence<Node> = sequence {
        this@Node.children.forEach {
            yieldAll(it.asSequence)
        }
        yield(this@Node)
    }

    override fun toString(): String {
        var name = ""
        for (c in this.constraints)
            name += (if (c.second > 0) "" else "!") + c.first + ", "
        return name.dropLast(2) + " = " + this.dominant
    }
}
