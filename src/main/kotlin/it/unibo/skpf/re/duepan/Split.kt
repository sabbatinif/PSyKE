package it.unibo.skpf.re.duepan

internal class Split(private val parent: Node, val children: Pair<Node, Node>) {

    val priority: Double
        get() = this.priority(this.parent)

    private fun priority(parent: Node): Double {
        val (trueNode, falseNode) = children
        var priority = -(trueNode.fidelity + falseNode.fidelity)

        listOf(trueNode, falseNode).forEach {
            if (parent.nClasses > it.nClasses)
                priority -= 100
        }
        if (trueNode.dominant == falseNode.dominant)
            priority += 200

        return priority
    }
}