import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import smile.data.DataFrame
import smile.data.type.DataTypes
import smile.io.Read
import java.util.stream.Stream
import kotlin.math.round

@Suppress("UNUSED_PARAMETER")
class DataFrameUtilsTest {

    data class TestContext(
        val dataset: DataFrame,
        val examplesPerClass: List<Int>,
        val first: Pair<String, Description>?,
        val classes: Set<Any>
    ) {
        fun onTest(action: TestContext.() -> Unit) {
            this.action()
        }

        override fun toString(): String =
            "TestContext(examplesPerClass=$examplesPerClass, first=$first, classes=$classes)"
    }

    @ParameterizedTest
    @ArgumentsSource(Companion::class)
    fun testRandomSplit(context: TestContext
//        dataset: DataFrame,
//        examplesPerClass: List<Int>,
//        first: Pair<String, Description>?,
//        classes: Set<Any>
    ) = context.onTest {
        val (train, test) = dataset.randomSplit(0.2)
        assertEquals(dataset.nrows(), train.nrows() + test.nrows())
        assertEquals(dataset.schema(), train.schema())
        assertEquals(dataset.schema(), test.schema())
        assertEquals(
            dataset.toStringSet(),
            train.toStringSet() + test.toStringSet()
        )
    }

//    @ParameterizedTest
//    @ArgumentsSource(Companion::class)
//    fun testInputs(
//        dataset: DataFrame,
//        examplesPerClass: List<Int>,
//        first: Pair<String, Description>?,
//        classes: Set<Any>
//    ) {
//        val inputs = dataset.inputs()
//        assertEquals(dataset.ncols() - 1, inputs.ncols())
//        assertEquals(dataset.nrows(), inputs.nrows())
//        dataset.toStringList().zip(inputs.toStringList()) { exp, act ->
//            assertEquals(
//                exp.substringBeforeLast(",") + "\n" + exp.substringAfterLast("\n"),
//                act
//            )
//        }
//    }
//
//    @ParameterizedTest
//    @ArgumentsSource(Companion::class)
//    fun testCategories(
//        dataset: DataFrame,
//        examplesPerClass: List<Int>,
//        first: Pair<String, Description>?,
//        classes: Set<Any>
//    ) {
//        assertEquals(classes, dataset.categories())
//    }
//
//    @ParameterizedTest
//    @ArgumentsSource(Companion::class)
//    fun testNCategories(
//        dataset: DataFrame,
//        examplesPerClass: List<Int>,
//        first: Pair<String, Description>?,
//        classes: Set<Any>
//    ) {
//        assertEquals(classes.size, dataset.nCategories())
//    }
//
//    @ParameterizedTest
//    @ArgumentsSource(Companion::class)
//    fun testOutputClasses(
//        dataset: DataFrame,
//        examplesPerClass: List<Int>,
//        first: Pair<String, Description>?,
//        classes: Set<Any>
//    ) {
//        assertEquals(dataset.nrows(), dataset.outputClasses().size)
//        dataset.outputClasses().forEach { dataset.categories().contains(it) }
//    }
//
//    @ParameterizedTest
//    @ArgumentsSource(Companion::class)
//    fun testClasses(
//        dataset: DataFrame,
//        examplesPerClass: List<Int>,
//        first: Pair<String, Description>?,
//        classes: Set<Any>
//    ) {
//        assertEquals(dataset.nrows(), dataset.classes().nrows())
//        dataset.classes().stream().forEach {
//            assert(it.getInt("Class") >= 0)
//            assert(it.getInt("Class") < classes.size)
//        }
//    }
//
//    @ParameterizedTest
//    @ArgumentsSource(Companion::class)
//    fun testOutputs(
//        dataset: DataFrame,
//        examplesPerClass: List<Int>,
//        first: Pair<String, Description>?,
//        classes: Set<Any>
//    ) {
//        val outputs = dataset.outputs()
//        assertEquals(dataset.ncols() - dataset.inputs().ncols(), outputs.ncols())
//        assertEquals(dataset.nrows(), outputs.nrows())
//        dataset.toStringList().zip(outputs.toStringList()) { exp, act ->
//            assertEquals("{" + exp.substringAfterLast(","), act)
//        }
//    }
//
//    @ParameterizedTest
//    @ArgumentsSource(Companion::class)
//    fun testDescribe(
//        dataset: DataFrame,
//        examplesPerClass: List<Int>,
//        first: Pair<String, Description>?,
//        classes: Set<Any>
//    ) {
//        fun Double.round() = round(this * 100) / 100
//
//        val description = dataset.describe()
//        assertEquals(dataset.inputs().schema().fields().count {
//            it.isNumeric
//        }, description.size)
//
//        if (first != null) {
//            val V1 = description[first.first]
//            val desc = first.second
//            assertNotNull(V1)
//            assertEquals(V1!!.min, desc.min)
//            assertEquals(V1.max, desc.max)
//            assertEquals(V1.mean.round(), desc.mean)
//            assertEquals(V1.stdDev.round(), desc.stdDev)
//        }
//    }
//
//    @ParameterizedTest
//    @ArgumentsSource(Companion::class)
//    fun testFilterByOutput(
//        dataset: DataFrame,
//        examplesPerClass: List<Int>,
//        first: Pair<String, Description>?,
//        classes: Set<Any>
//    ) {
//        dataset.categories().zip(examplesPerClass) { cat, ex ->
//            val filtered = dataset.filterByOutput(cat)
//            assertEquals(ex, filtered.nrows())
//            assertEquals(dataset.schema(), filtered.schema())
//            assert(dataset.toStringList().containsAll(filtered.toStringList()))
//            assertEquals(1, filtered.categories().size)
//            assertEquals(cat, filtered.categories().first())
//        }
//    }
//
//    @ParameterizedTest
//    @ArgumentsSource(Companion::class)
//    fun testWriteColumn(
//        dataset: DataFrame,
//        examplesPerClass: List<Int>,
//        first: Pair<String, Description>?,
//        classes: Set<Any>
//    ) {
//        val firstFeature = dataset.schema().fields().first()
//        val value = when (firstFeature.type) {
//            DataTypes.DoubleType -> 5.0
//            DataTypes.StringType -> "5.0"
//            DataTypes.IntegerType -> 5
//            else -> Any()
//        }
//        val written = dataset.writeColumn(firstFeature.name, value)
//        assertEquals(dataset.schema(), written.schema())
//        assertEquals(dataset.nrows(), written.stream().filter { it[firstFeature.name] == value }.count().toInt())
//        assertEquals(dataset.drop(firstFeature.name).toStringList(), written.drop(firstFeature.name).toStringList())
//    }
//
//    @ParameterizedTest
//    @ArgumentsSource(Companion::class)
//    fun testCreateRanges(
//        dataset: DataFrame,
//        examplesPerClass: List<Int>,
//        first: Pair<String, Description>?,
//        classes: Set<Any>
//    ) {
//        dataset.inputs().schema().fields()
//            .filter { it.type == DataTypes.DoubleType }
//            .map { it.name }.forEach {
//                val ranges = dataset.createRanges(it)
//                val description = dataset.describe()[it]
//                ranges.zipWithNext { prev, foll ->
//                    assertEquals(prev.upper, foll.lower)
//                }
//                assert(ranges.first().lower < description!!.min)
//                assert(ranges.last().upper > description.max)
//            }
//    }
//
//    @ParameterizedTest
//    @ArgumentsSource(Companion::class)
//    fun testSplitFeatures(
//        dataset: DataFrame,
//        examplesPerClass: List<Int>,
//        first: Pair<String, Description>?,
//        classes: Set<Any>
//    ) {
//        val featureSet = dataset.splitFeatures()
//        assertEquals(dataset.inputs().ncols(), featureSet.size)
//        dataset.inputs().schema().fields().map { it.name }.forEach {
//            assert(featureSet.map { it.name }.contains(it))
//        }
//        featureSet.forEach {
//            when (dataset.schema().field(it.name).type) {
//                DataTypes.DoubleType -> assertEquals(classes.size, it.set.size)
//                else -> assertEquals(dataset.column(it.name).toStringArray().distinct().size, it.set.size)
//            }
//
//        }
//    }
//
//    @ParameterizedTest
//    @ArgumentsSource(Companion::class)
//    fun testToBoolean(
//        dataset: DataFrame,
//        examplesPerClass: List<Int>,
//        first: Pair<String, Description>?,
//        classes: Set<Any>
//    ) {
//        val featureSet = dataset.splitFeatures()
//        val boolDataset = dataset.toBoolean(featureSet)
//        assertEquals(dataset.nrows(), boolDataset.nrows())
//        assertEquals(dataset.outputs().toStringList(), boolDataset.outputs().toStringList())
//        assertEquals(
//            featureSet.fold(0) { acc, it -> acc + it.set.size },
//            boolDataset.inputs().ncols()
//        )
//        featureSet.forEach {
//            val subDataset = boolDataset.select(*it.set.keys.toTypedArray())
//            assertEquals(it.set.size, subDataset.ncols())
//            subDataset.stream().forEach {
//                assertEquals(1, it.toArray().count { it == 1.0 })
//                assertEquals(subDataset.ncols() - 1, it.toArray().count { it == 0.0 })
//            }
//        }
//    }

    companion object : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            val iris = Read.csv("datasets/iris.data")
            val car = Read.csv("datasets/car.data")
            return Stream.of(
                TestContext(
                    iris,
                    listOf(50, 50, 50),
                    "V1" to Description(5.84, 0.83, 4.3, 7.9),
                    setOf("Iris-setosa", "Iris-versicolor", "Iris-virginica")
                ),
                TestContext(
                    car,
                    listOf(1210, 384, 65, 69),
                    null,
                    setOf("vgood", "good", "acc", "unacc")
                )
            ).map { Arguments.of(it) }
        }
    }
}