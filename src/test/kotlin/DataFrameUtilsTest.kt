import junit.framework.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import smile.data.DataFrame
import smile.data.type.DataTypes
import smile.io.Read
import kotlin.math.round
import kotlin.streams.toList

@RunWith(Parameterized::class)
class DataFrameUtilsTest(private val dataset: DataFrame,
                         private val examplesPerClass: List<Int>,
                         private val first: Pair<String, Description>?,
                         private val classes: Set<Any>
) {
    private val nclasses = this.classes.size

    @Test
    fun testRandomSplit() {
        val (train, test) = dataset.randomSplit(0.2)
        assertEquals(dataset.nrows(), train.nrows() + test.nrows())
        assertEquals(dataset.schema(), train.schema())
        assertEquals(dataset.schema(), test.schema())
        assertEquals(dataset.toStringSet(),
            train.toStringSet().plus(test.toStringSet())
        )
    }

    @Test
    fun testInputs() {
        val inputs = dataset.inputs()
        assertEquals(dataset.ncols() - 1, inputs.ncols())
        assertEquals(dataset.nrows(), inputs.nrows())
        dataset.toStringList().zip(inputs.toStringList()) { exp, act ->
            assertEquals(exp.substringBeforeLast(",") +
                    "\n" + exp.substringAfterLast("\n"),
                act
            )
        }
    }

    @Test
    fun testCategories() {
        assertEquals(classes, dataset.categories())
    }

    @Test
    fun testNCategories() {
        assertEquals(nclasses, dataset.nCategories())
    }

    @Test
    fun testOutputClasses() {
        assertEquals(dataset.nrows(), dataset.outputClasses().size)
        dataset.outputClasses().forEach { dataset.categories().contains(it) }
    }

    @Test
    fun testClasses() {
        assertEquals(dataset.nrows(), dataset.classes().nrows())
        dataset.classes().stream().forEach {
            assert(it.getInt("Class") >= 0)
            assert(it.getInt("Class") < nclasses)
        }
    }

    @Test
    fun testOutputs() {
        val outputs = dataset.outputs()
        assertEquals(dataset.ncols() - dataset.inputs().ncols(), outputs.ncols())
        assertEquals(dataset.nrows(), outputs.nrows())
        dataset.toStringList().zip(outputs.toStringList()) { exp, act ->
            assertEquals("{" + exp.substringAfterLast(","),
                act
            )
        }
    }

    @Test
    fun testDescribe() {
        fun Double.round() = round(this * 100) / 100

        val description = dataset.describe()
        assertEquals(dataset.inputs().schema().fields().count {
            it.isNumeric
        }, description.size)

        if (first != null) {
            val V1 = description[first.first]
            val desc = first.second
            assertNotNull(V1)
            assertEquals(V1!!.min, desc.min)
            assertEquals(V1.max, desc.max)
            assertEquals(V1.mean.round(), desc.mean)
            assertEquals(V1.std.round(), desc.std)
        }
    }

    @Test
    fun testFilterByOutput() {
        dataset.categories().zip(examplesPerClass) { cat, ex ->
            val filtered = dataset.filterByOutput(cat)
            assertEquals(ex, filtered.nrows())
            assertEquals(dataset.schema(), filtered.schema())
            assert(dataset.toStringList().containsAll(filtered.toStringList()))
            assertEquals(1, filtered.categories().size)
            assertEquals(cat, filtered.categories().first())
        }
    }

    @Test
    fun testWriteColumn() {
        val firstFeature = dataset.schema().fields().first()
        val value = when (firstFeature.type) {
            DataTypes.DoubleType -> 5.0
            DataTypes.StringType -> "5.0"
            DataTypes.IntegerType -> 5
            else -> Any()
        }
        val written = dataset.writeColumn(firstFeature.name, value)
        assertEquals(dataset.schema(), written.schema())
        assertEquals(dataset.nrows(),
            written.stream().toList().count { it[firstFeature.name] == value })
        assertEquals(dataset.drop(firstFeature.name).toStringList(),
            written.drop(firstFeature.name).toStringList())
    }

    @Test
    fun testCreateRanges() {
        dataset.inputs().schema().fields()
            .filter { it.type == DataTypes.DoubleType }
            .map { it.name }.forEach {
                val ranges = dataset.createRanges(it)
                val description = dataset.describe()[it]
                ranges.zipWithNext { prev, foll ->
                    assertEquals(prev.upper, foll.lower)
                }
                assert(ranges.first().lower < description!!.min)
                assert(ranges.last().upper > description.max)
        }
    }

    @Test
    fun testSplitFeatures() {
        val featureSet = dataset.splitFeatures()
        assertEquals(dataset.inputs().ncols(), featureSet.size)
        dataset.inputs().schema().fields().map { it.name }.forEach {
            assert(featureSet.map { it.name }.contains(it))
        }
        featureSet.forEach {
            when (dataset.schema().field(it.name).type) {
                DataTypes.DoubleType -> assertEquals(nclasses, it.set.size)
                else -> assertEquals(dataset.column(it.name).toStringArray().distinct().size, it.set.size)
            }

        }
    }

    @Test
    fun testToBoolean() {
        val featureSet = dataset.splitFeatures()
        val boolDataset = dataset.toBoolean(featureSet)
        assertEquals(dataset.nrows(), boolDataset.nrows())
        assertEquals(dataset.outputs().toStringList(), boolDataset.outputs().toStringList())
        assertEquals(
            featureSet.fold(0) { acc, it -> acc + it.set.size },
            boolDataset.inputs().ncols()
        )
        featureSet.forEach {
            val subDataset = boolDataset.select(*it.set.keys.toTypedArray())
            assertEquals(it.set.size, subDataset.ncols())
            subDataset.stream().forEach {
                assertEquals(1, it.toArray().count { it == 1.0 })
                assertEquals(subDataset.ncols() - 1, it.toArray().count { it == 0.0 })
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun parameters(): Collection<*> {
            return listOf(
                arrayOf(Read.csv("datasets/iris.data"),
                    listOf(50, 50, 50),
                    "V1" to Description(5.84, 0.83, 4.3, 7.9),
                    setOf("Iris-setosa", "Iris-versicolor", "Iris-virginica")
                ),
                arrayOf(Read.csv("datasets/car.data"),
                    listOf(1210, 384, 65, 69), null,
                    setOf("vgood", "good", "acc", "unacc")
                )
            )
        }
    }
}