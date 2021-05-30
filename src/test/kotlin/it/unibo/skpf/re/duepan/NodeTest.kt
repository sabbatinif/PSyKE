package it.unibo.skpf.re.duepan

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import smile.io.Read

class NodeTest {
    private val dataset = Read.csv("datasets/iris.data")
    private val nExamples = dataset.nrows()
    private val nodeAll = Node(dataset, nExamples)
    private val node40Setosa = Node(dataset.slice(10, 70), nExamples)
    private val node10Virginica = Node(dataset.slice(95, 110), nExamples)
    private val node50Versicolor = Node(dataset.slice(20, 130), nExamples)

    @Test
    fun testReach() {
        val node = Node(dataset, nExamples)
        assert(node.reach == nodeAll.reach)
        assert(node10Virginica.reach < node40Setosa.reach)
        assert(node40Setosa.reach < node50Versicolor.reach)
        assert(node50Versicolor.reach < nodeAll.reach)
    }

    @Test
    fun testDominant() {
        assertEquals("Iris-setosa", node40Setosa.dominant)
        assertEquals("Iris-virginica", node10Virginica.dominant)
        assertEquals("Iris-versicolor", node50Versicolor.dominant)
    }

    @Test
    fun testCorrect() {
        assertEquals(50, node50Versicolor.correct)
        assertEquals(40, node40Setosa.correct)
        assertEquals(10, node10Virginica.correct)
    }

    @Test
    fun testFidelity() {
        assertEquals(50.0 / 150.0, nodeAll.fidelity)
        assertEquals(40.0 / 60.0, node40Setosa.fidelity)
        assertEquals(10.0 / 15.0, node10Virginica.fidelity)
        assertEquals(50.0 / 110.0, node50Versicolor.fidelity)
    }

    @Test
    fun testPriority() {
        assert(nodeAll.priority < node50Versicolor.priority)
        assert(node50Versicolor.priority < node40Setosa.priority)
        assert(node40Setosa.priority < node10Virginica.priority)
    }

    @Test
    fun testNClasses() {
        assertEquals(3, nodeAll.nClasses)
        assertEquals(2, node10Virginica.nClasses)
        assertEquals(2, node40Setosa.nClasses)
        assertEquals(3, node50Versicolor.nClasses)
        assertEquals(1, Node(dataset.slice(15, 40), nExamples).nClasses)
    }

    @Test
    fun testAsSequence() {
        val node = Node(dataset, nExamples)
        val child1 = Node(dataset.slice(0, 50), nExamples)
        val child2 = Node(dataset.slice(50, 150), nExamples)
        node.children = mutableListOf(child1, child2)
        val grandchild11 = Node(dataset.slice(0, 25), nExamples)
        val grandchild21 = Node(dataset.slice(50, 80), nExamples)
        val grandchild22 = Node(dataset.slice(80, 120), nExamples)
        child1.children = mutableListOf(grandchild11)
        child2.children = mutableListOf(grandchild21, grandchild22)
        assertEquals(
            node.asSequence.toList(),
            child1.asSequence.toList() + child2.asSequence.toList() + node
        )
        assertEquals(
            listOf(grandchild11, child1, grandchild21, grandchild22, child2, node),
            node.asSequence.toList()
        )
    }

    @Test
    fun testToString() {
        val node = Node(
            dataset, nExamples, setOf("V1" to 0.0, "V2" to 1.0)
        )
        assertEquals(" = Iris-setosa", nodeAll.toString())
        assertEquals(" = Iris-setosa", node40Setosa.toString())
        assertEquals(" = Iris-virginica", node10Virginica.toString())
        assertEquals(" = Iris-versicolor", node50Versicolor.toString())
        assertEquals("!V1, V2 = Iris-setosa", node.toString())
    }
}