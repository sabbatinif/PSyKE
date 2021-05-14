import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Term
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.theory.MutableTheory
import smile.classification.Classifier
import smile.data.DataFrame
import it.unibo.tuprolog.theory.Theory
import smile.data.Tuple
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.math.sign
import kotlin.streams.toList

class Duepan(override val predictor: Classifier<DoubleArray>,
             override val dataset: DataFrame,
             override val featureSet: Set<BooleanFeatureSet>,
             val maxSize: Int = 9999,
             val minExamples: Int = 0
) : Extractor<DoubleArray, Classifier<DoubleArray>> {

    class Split(
        val priority: Double,
        val children: Pair<Node, Node>
    )

    private lateinit var root: Node

    override fun extract(x: Array<DoubleArray>): Theory {
        this.root = Node(this.dataset, this.dataset.nrows(), emptySet())

        val queue: SortedSet<Node> =
            sortedSetOf(kotlin.Comparator { n1, n2 ->
                (n1.priority() - n2.priority()).sign.toInt()
            })
        queue.add(this.root)

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

    fun bestSplit(node: Node): Pair<Node, Node>? {
        if (node.nClasses() == 1)
            return null

        val splits: SortedSet<Split> =
            sortedSetOf(kotlin.Comparator { s1, s2 ->
                (s1.priority - s2.priority).sign.toInt()
            })

        val constraints = node.constraints.map { it.first }.toSet()

        for (column in (this.dataset.inputs().names().filter { !constraints.contains(it) }))
        {
            try {
                val trueExamples = DataFrame.of(node.samples.stream().filter{
                    it[column] == 1.0
                })
                val falseExamples = DataFrame.of(node.samples.stream().filter{
                    it[column] == 0.0
                })

                val trueConstraints = node.constraints.plus(Pair(column, 1.0))
                val falseConstraints = node.constraints.plus(Pair(column, 0.0))

                val trueNode = Node(trueExamples, node.nExamples, trueConstraints)
                val falseNode = Node( falseExamples, node.nExamples, falseConstraints)

                var splitPriority = -(trueNode.fidelity() + falseNode.fidelity())

                listOf(trueNode, falseNode).forEach {
                    if (node.nClasses() > it.nClasses())
                        splitPriority -= 100
                }
                if (trueNode.dominant() == falseNode.dominant())
                    splitPriority += 200

                splits.add(Split(splitPriority, Pair(trueNode, falseNode)))

            } catch (e: IndexOutOfBoundsException) { continue }
        }
        if (splits.isEmpty())
            return null
        return splits.first().children
    }

    fun predict(x: Tuple, node: Node): Int {
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

    fun optimize() {
        val nodes = mutableListOf(this.root)
        var n = 0
        while (nodes.isNotEmpty()) {
            val node = nodes.removeAt(0)
            val toRemove = mutableListOf<Node>()
            for (child in node.children) {
                if (child.children.isEmpty()) {
                    if (node.dominant() == child.dominant()) {
                        toRemove.add(child)
                        n++
                    }
                }
                else
                    nodes.add(child)
            }
            node.children.removeAll(toRemove)
        }
        if (n > 0)
            this.optimize()
    }

    fun compact() {
        val nodes = mutableListOf(this.root)
        while (nodes.isNotEmpty()) {
            val node = nodes.removeAt(0)
            val toRemove = mutableListOf<Node>()
            node.children.forEach {
                if ((node.dominant() == it.dominant()) &&
                    (it.children.size == 1)) {
                    toRemove.add(it)
                    nodes.add(node)
                }
                else
                    nodes.add(it)
            }
            while (toRemove.isNotEmpty()) {
                val n = toRemove.removeAt(0)
                node.children.remove(n)
                node.children.addAll(n.children)
            }
        }
    }

    fun printNodes(node: Node) {
        println(node.toString())
        node.children.forEach {
            printNodes(it)
        }
    }

    override fun createTheory(): MutableTheory {
        fun ruleFromNode(node: Node = this.root,
                         theory: MutableTheory = MutableTheory.empty()
        ): MutableTheory {
            node.children.forEach{
                ruleFromNode(it, theory)
            }
            val variables = createVariableList(this.featureSet)
            val head = createHead("concept", variables.values, node.dominant().toString())
            val body: MutableList<Term> = mutableListOf()
            for ((constraint, value) in node.constraints) {
                this.featureSet.filter { it.set.containsKey(constraint) }.first().apply {
                    body.add(
                        createTerm(
                            variables[this.name] ?: Var.of(this.name),
                            this.set[constraint] ?: Any(),
                            value == 1.0
                        )
                    )
                }
            }
            theory.assertZ(Clause.of(head, *body.toTypedArray()))
            return theory
        }
        return ruleFromNode()
    }
}