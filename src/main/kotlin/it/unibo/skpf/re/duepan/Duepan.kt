package it.unibo.skpf.re.duepan

import it.unibo.skpf.re.*
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Term
import it.unibo.tuprolog.theory.MutableTheory
import it.unibo.tuprolog.theory.Theory
import smile.classification.Classifier
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.categories
import smile.data.inputs
import java.util.*
import kotlin.math.sign
import kotlin.streams.toList

internal class Duepan(
    override val predictor: Classifier<DoubleArray>,
    override val featureSet: Set<BooleanFeatureSet>,
    val minExamples: Int = 0
) : Extractor<DoubleArray, Classifier<DoubleArray>> {

    private lateinit var root: Node

    private fun init(dataset: DataFrame): SortedSet<Node> {
        this.root = Node(dataset, dataset.nrows())
        val queue: SortedSet<Node> =
            sortedSetOf(kotlin.Comparator { n1, n2 ->
                (n1.priority - n2.priority).sign.toInt()
            })
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
        this.compact()
        return this.createTheory()
    }

    private fun initSplits(node: Node) = Pair(
        sortedSetOf<Split>(kotlin.Comparator { s1, s2 ->
            (s1.priority - s2.priority).sign.toInt()
        }),
        node.constraints.map { it.first }.toSet()
    )

    private fun createSplits(node: Node, names: Array<String>): SortedSet<Split> {
        val (splits, constraints)  = initSplits(node)
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
            return null //println("Pochi esempi")
        if (node.nClasses == 1)
            return null
        val splits = createSplits(node, names)
        return if (splits.isEmpty()) null else splits.first().children
    }

    private fun createSamples(node: Node, column: String, value: Double) =
        DataFrame.of(node.samples.stream().filter {
            it[column] == value
        })

    private fun createSplit(node: Node, column: String): Split {
        val trueExamples = createSamples(node, column, 1.0)
        val falseExamples = createSamples(node, column, 0.0)
        val trueConstraints = node.constraints.plus(Pair(column, 1.0))
        val falseConstraints = node.constraints.plus(Pair(column, 0.0))
        val trueNode = Node(trueExamples, node.nExamples, trueConstraints)
        val falseNode = Node(falseExamples, node.nExamples, falseConstraints)
        return Split(node, trueNode to falseNode)
    }

    private fun predict(x: Tuple, node: Node, categories: Set<Any>): Int {
        for (child in node.children) {
            var exit = false
            for ((constraint, value) in child.constraints)
                if (x[constraint] != value) {
                    exit = true
                    break
                }
            if (!exit)
                return this.predict(x, child, categories)
        }
        return categories.indexOf(node.dominant)
    }

    override fun predict(dataset: DataFrame): IntArray {
        return dataset.stream().map { this.predict(it, this.root, dataset.categories()) }.toList().toIntArray()
    }

    private fun optimize() {
        val nodes = mutableListOf(this.root)
        var n = 0
        while (nodes.isNotEmpty()) {
            val node = nodes.removeAt(0)
            val toRemove = mutableListOf<Node>()
            node.children.filter { it.children.isEmpty() && node.dominant == it.dominant }
                .forEach {
                    toRemove.add(it)
                    n++
                }
            node.children.removeAll(toRemove)
            node.children.filter { it.children.isNotEmpty() }.forEach { nodes.add(it) }
        }
        if (n > 0)
            this.optimize()
    }

    private fun compact() {
        val nodes = mutableListOf(this.root)
        while (nodes.isNotEmpty()) {
            val node = nodes.removeAt(0)
            val toRemove = mutableListOf<Node>()
            node.children.forEach {
                if ((node.dominant == it.dominant) &&
                    (it.children.size == 1)
                ) {
                    toRemove.add(it)
                    nodes.add(node)
                } else
                    nodes.add(it)
            }
            while (toRemove.isNotEmpty()) {
                val n = toRemove.removeAt(0)
                node.children.remove(n)
                node.children.addAll(n.children)
            }
        }
    }

    private fun createTheory(): MutableTheory {
        val variables = createVariableList(this.featureSet)
        val theory = MutableTheory.empty()
        this.root.asSequence.forEach {
            val head = createHead("concept", variables.values, it.dominant.toString())
            val body: MutableList<Term> = mutableListOf()
            for ((constraint, value) in it.constraints) {
                this.featureSet.first { it.set.containsKey(constraint) }.apply {
                    body.add(
                        createTerm(variables[this.name], this.set[constraint], value == 1.0)
                    )
                }
            }
            theory.assertZ(Clause.of(head, *body.toTypedArray()))
        }
        return theory
    }
}