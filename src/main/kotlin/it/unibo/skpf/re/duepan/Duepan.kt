package it.unibo.skpf.re.duepan

import it.unibo.skpf.re.BooleanFeatureSet
import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.utils.createHead
import it.unibo.skpf.re.utils.createTerm
import it.unibo.skpf.re.utils.createVariableList
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.theory.MutableTheory
import it.unibo.tuprolog.theory.Theory
import smile.classification.Classifier
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.categories
import smile.data.inputs
import smile.data.name
import java.util.SortedSet
import kotlin.math.sign

internal class Duepan(
    override val predictor: Classifier<DoubleArray>,
    override val featureSet: Collection<BooleanFeatureSet>,
    val minExamples: Int = 0
) : Extractor<DoubleArray, Classifier<DoubleArray>> {

    private lateinit var root: Node

    private fun init(dataset: DataFrame): SortedSet<Node> {
        this.root = Node(dataset, dataset.nrows())
        val queue: SortedSet<Node> =
            sortedSetOf(
                kotlin.Comparator { n1, n2 ->
                    (n1.priority - n2.priority).sign.toInt()
                }
            )
        queue.add(this.root)
        return queue
    }

    override fun extract(dataset: DataFrame): Theory {
        val queue = this.init(dataset)
        while (queue.isNotEmpty()) {
            val node = queue.first()
            queue.remove(node)
            val best = this.bestSplit(node, dataset.inputs().names()) ?: continue
            queue.addAll(best.toList())
            node.children.addAll(best.toList())
        }
        this.optimize()
        return this.createTheory(dataset.name())
    }

    private fun initSplits(node: Node) = Pair(
        sortedSetOf<Split>({ s1, s2 ->
            (s1.priority - s2.priority).sign.toInt()
        }),
        node.constraints.map { it.first }.toSet()
    )

    private fun createSplits(node: Node, names: Array<String>): SortedSet<Split> {
        val (splits, constraints) = initSplits(node)
        for (column in (names.filterNot { constraints.contains(it) }))
            try {
                splits.add(this.createSplit(node, column))
            } catch (e: IndexOutOfBoundsException) {
                continue
            }
        return splits
    }

    private fun bestSplit(node: Node, names: Array<String>): Pair<Node, Node>? {
        if (node.samples.nrows() < this.minExamples)
            throw NotImplementedError()
        if (node.nClasses == 1)
            return null
        val splits = createSplits(node, names)
        return if (splits.isEmpty()) null else splits.first().children
    }

    private fun createSamples(node: Node, column: String, value: Double) =
        DataFrame.of(
            node.samples.stream().filter {
                it[column] == value
            }
        )

    private fun createSplit(node: Node, column: String): Split {
        val trueExamples = createSamples(node, column, 1.0)
        val falseExamples = createSamples(node, column, 0.0)
        val trueConstraints = node.constraints.plus(Pair(column, 1.0))
        val falseConstraints = node.constraints.plus(Pair(column, 0.0))
        val trueNode = Node(trueExamples, node.nExamples, trueConstraints)
        val falseNode = Node(falseExamples, node.nExamples, falseConstraints)
        return Split(node, trueNode to falseNode)
    }

    private tailrec fun predict(x: Tuple, node: Node, categories: Set<Any>): Int {
        nextChild@for (child in node.children) {
            for ((constraint, value) in child.constraints)
                if (x[constraint] != value)
                    continue@nextChild
            return this.predict(x, child, categories)
        }
        return categories.indexOf(node.dominant)
    }

    override fun predict(dataset: DataFrame): Array<*> =
        dataset.stream().map {
            this.predict(it, this.root, dataset.categories())
        }.toArray()

    private fun removeNodes(nodes: MutableList<Node>): Int {
        val node = nodes.removeAt(0)
        val toRemove = mutableListOf<Node>()
        node.children.filter { it.children.isEmpty() && node.dominant == it.dominant }
            .forEach { toRemove.add(it) }
        node.children.removeAll(toRemove)
        node.children.filter { it.children.isNotEmpty() }.forEach { nodes.add(it) }
        return toRemove.size
    }

    private tailrec fun optimize() {
        val nodes = mutableListOf(this.root)
        var n = 0
        while (nodes.isNotEmpty())
            n += removeNodes(nodes)
        if (n == 0)
            this.compact()
        else
            this.optimize()
    }

    private fun nodesToRemove(node: Node, nodes: MutableList<Node>): MutableList<Node> {
        val toRemove = mutableListOf<Node>()
        for (child in node.children)
            if ((node.dominant == child.dominant) && (child.children.size == 1)) {
                toRemove.add(child)
                nodes.add(node)
            } else
                nodes.add(child)
        return toRemove
    }

    private fun compact() {
        val nodes = mutableListOf(this.root)
        while (nodes.isNotEmpty()) {
            val node = nodes.removeAt(0)
            nodesToRemove(node, nodes).forEach {
                node.children.remove(it)
                node.children.addAll(it.children)
            }
        }
    }

    private fun createBody(variables: Map<String, Var>, node: Node) = sequence {
        for ((constraint, value) in node.constraints)
            featureSet.first { it.set.containsKey(constraint) }.apply {
                yield(createTerm(variables[this.name], this.set[constraint]!!, value == 1.0))
            }
    }.toList().toTypedArray()

    private fun createTheory(name: String): MutableTheory {
        val variables = createVariableList(this.featureSet)
        val theory = MutableTheory.empty()
        for (node in this.root.asSequence)
            theory.assertZ(
                Clause.of(
                    createHead(name, variables.values, node.dominant.toString()),
                    *createBody(variables, node)
                )
            )
        return theory
    }
}
