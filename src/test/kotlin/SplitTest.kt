import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import smile.io.Read

class SplitTest {
    private val dataset = Read.csv("datasets/iris.data")
    private val nExamples = dataset.nrows()
    private val nodeAll = Node(dataset, nExamples)
    private val node40Setosa = Node(dataset.slice(10, 70), nExamples)
    private val node40SetosaCompl = Node(dataset.slice(0, 10).union(dataset.slice(70, 150)), nExamples)
    private val node25Virginica = Node(dataset.slice(40, 75), nExamples)
    private val node25VirginicaCompl = Node(dataset.slice(75, 110), nExamples)

    @Test
    fun testPriority() {
        assertEquals(
            -40.0 / 60.0 - 50.0 / 90.0 - 100.0,
            Split(nodeAll, Pair(node40Setosa, node40SetosaCompl)).priority,
            0.00001
        )
        assertEquals(
            (25.0 / 35.0) * -2 - 200.0 + 200.0,
            Split(nodeAll, Pair(node25Virginica, node25VirginicaCompl)).priority,
            0.00001
        )
    }
}