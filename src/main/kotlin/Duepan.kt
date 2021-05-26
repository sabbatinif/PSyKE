import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Term
import it.unibo.tuprolog.theory.MutableTheory
import it.unibo.tuprolog.theory.Theory
import smile.classification.Classifier
import smile.data.DataFrame
import smile.data.Tuple
import java.util.*
import kotlin.math.sign
import kotlin.streams.toList

internal class Duepan(
    override val predictor: Classifier<DoubleArray>,
    override val featureSet: Set<BooleanFeatureSet>,
    val minExamples: Int = 0
) : Extractor<DoubleArray, Classifier<DoubleArray>> {

    private lateinit var dataset: DataFrame
    private lateinit var root: Node

    private fun init(x: DataFrame): SortedSet<Node> {
        this.dataset = x
        this.root = Node(this.dataset, this.dataset.nrows())

        val queue: SortedSet<Node> =
            sortedSetOf(kotlin.Comparator { n1, n2 ->
                (n1.priority() - n2.priority()).sign.toInt()
            })
        queue.add(this.root)
        return queue
    }

    override fun extract(x: DataFrame): Theory {
        val queue = this.init(x)

        while (queue.isNotEmpty()) {
            val node = queue.first()
            queue.remove(node)

            if (node.samples.nrows() < this.minExamples)
                continue
            //println("Pochi esempi")

            val best = this.bestSplit(node) ?: continue
            queue.addAll(best.toList())
            node.children.addAll(best.toList())
        }
        this.optimize()
        this.compact()
        return this.createTheory()
    }

    private fun bestSplit(node: Node): Pair<Node, Node>? {
        if (node.nClasses() == 1)
            return null

        val splits: SortedSet<Split> =
            sortedSetOf(kotlin.Comparator { s1, s2 ->
                (s1.priority - s2.priority).sign.toInt()
            })

        val constraints = node.constraints.map { it.first }.toSet()

        for (column in (this.dataset.inputs().names().filterNot { constraints.contains(it) }))
            try {
                splits.add(this.createSplit(node, column))
            } catch (e: IndexOutOfBoundsException) {
                continue
            }
        return if (splits.isEmpty()) null else splits.first().children
    }

    private fun createSplit(node: Node, column: String): Split {
        val trueExamples = DataFrame.of(node.samples.stream().filter {
            it[column] == 1.0
        })
        val falseExamples = DataFrame.of(node.samples.stream().filter {
            it[column] == 0.0
        })

        val trueConstraints = node.constraints.plus(Pair(column, 1.0))
        val falseConstraints = node.constraints.plus(Pair(column, 0.0))

        val trueNode = Node(trueExamples, node.nExamples, trueConstraints)
        val falseNode = Node(falseExamples, node.nExamples, falseConstraints)

        return Split(node, trueNode to falseNode)
    }

    private fun predict(x: Tuple, node: Node): Int {
        for (child in node.children) {
            var exit = false
            for ((constraint, value) in child.constraints) {
                if (x[constraint] != value) {
                    exit = true
                    break
                }
            }
            if (!exit)
                return this.predict(x, child)
        }
        return this.dataset.categories().indexOf(node.dominant())
    }

    override fun predict(x: DataFrame): IntArray {
        return x.stream().map { this.predict(it, this.root) }.toList().toIntArray()
    }

    private fun optimize() {
        val nodes = mutableListOf(this.root)
        var n = 0
        while (nodes.isNotEmpty()) {
            val node = nodes.removeAt(0)
            val toRemove = mutableListOf<Node>()
            node.children.filter { it.children.isEmpty() && node.dominant() == it.dominant() }
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
                if ((node.dominant() == it.dominant()) &&
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

        fun ruleFromNode(
            node: Node = this.root,
            theory: MutableTheory = MutableTheory.empty()
        ): MutableTheory {
            node.children.forEach {
                ruleFromNode(it, theory)
            }
            val head = createHead("concept", variables.values, node.dominant().toString())
            val body: MutableList<Term> = mutableListOf()
            for ((constraint, value) in node.constraints) {
                this.featureSet.first { it.set.containsKey(constraint) }.apply {
                    body.add(
                        createTerm(variables[this.name], this.set[constraint], value == 1.0)
                    )
                }
            }
            theory.assertZ(Clause.of(head, *body.toTypedArray()))
            return theory
        }
        return ruleFromNode()
    }
}