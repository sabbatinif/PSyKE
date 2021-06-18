package smile.data

import it.unibo.skpf.re.schema.DiscreteFeature
import it.unibo.skpf.re.schema.Discretization
import it.unibo.skpf.re.utils.createColumn
import it.unibo.skpf.re.utils.createDescriptionPair
import it.unibo.skpf.re.utils.createSet
import it.unibo.skpf.re.utils.expandRanges
import smile.data.type.DataTypes
import smile.data.type.StructField
import smile.data.type.StructType
import smile.data.vector.BaseVector
import kotlin.random.Random
import kotlin.streams.toList

/**
 * Splits this DataFrame into a Pair of DataFrame.
 * @param percent is the test set percentage.
 * @param seed (optional) is the random seed.
 * @return a Pair with the training and test partitions.
 */
fun DataFrame.randomSplit(percent: Double, seed: Long = 10L): Pair<DataFrame, DataFrame> {
    val r1 = Random(seed)
    val r2 = Random(seed)
    var train: DataFrame
    var test: DataFrame
    this.stream().toList().apply {
        train = DataFrame.of(this.filter { r1.nextDouble() >= percent })
        test = DataFrame.of(this.filter { r2.nextDouble() < percent })
    }
    return train to test
}

/**
 * The index of the last column of this DataFrame.
 */
val DataFrame.lastColumnIndex: Int
    get() = this.ncols() - 1

/**
 * Selects only the input DataFrame columns.
 * @param outputColumn (optional) is the index of the output column.
 * @return a DataFrame with only the input columns.
 */
fun DataFrame.inputs(outputColumn: Int = lastColumnIndex): DataFrame =
    this.drop(outputColumn)

/**
 * Generates a set with the output column distinct values.
 * @param outputColumn (optional) is the index of the output column.
 * @return the set of distinct output values.
 */
fun DataFrame.categories(outputColumn: Int = lastColumnIndex): Set<Any> =
    this.outputClasses(outputColumn).distinct().toSet()

/**
 * Counts the number of distinct output classes.
 * @param outputColumn (optional) is the index of the output column.
 * @return the number of output classes.
 */
fun DataFrame.nCategories(outputColumn: Int = lastColumnIndex): Int =
    this.categories(outputColumn).size

/**
 * Creates a list from the DataFrame output column.
 * @param outputColumn (optional) is the index of the output column.
 * @return the output column as list.
 */
fun DataFrame.outputClasses(outputColumn: Int = lastColumnIndex): List<Any> =
    this.stream().map { it[outputColumn] }.toList()

/**
 * Selects the output column from this DataFrame and converts its values to IntegerType.
 * @param outputColumn (optional) is the index of the output column.
 * @return a DataFrame with the only output column as IntegerType.
 */
fun DataFrame.classes(outputColumn: Int = lastColumnIndex): DataFrame {
    val classes = categories(outputColumn)
    return DataFrame.of(
        this.select(outputColumn).stream().map {
            val klass = classes.indexOf(it[0])
            require(klass >= 0) { "Invalid class: ${it[0]}" }
            Tuple.of(arrayOf(klass), StructType(StructField("Class", DataTypes.IntegerType)))
        }
    )
}

/**
 * Selects the output column from this DataFrame.
 * @param outputColumn (optional) is the index of the output column.
 * @return a DataFrame with the only output column.
 */
fun DataFrame.outputs(outputColumn: Int = lastColumnIndex): DataFrame =
    this.select(outputColumn)

/**
 * Selects the input columns from this DataFrame and converts it into an Array of DoubleArray.
 * @param outputColumn (optional) is the index of the output column.
 * @return the input columns as an Array of DoubleArray
 */
fun DataFrame.inputsArray(outputColumn: Int = lastColumnIndex): Array<DoubleArray> =
    this.inputs(outputColumn).toArray()

/**
 * Selects the output column from this DataFrame and converts it into a DoubleArray.
 * @param outputColumn (optional) is the index of the output column.
 * @return the output column as a DoubleArray
 */
fun DataFrame.outputsArray(outputColumn: Int = lastColumnIndex): DoubleArray =
    this.column(outputColumn).toDoubleArray()

/**
 * Selects the output column from this DataFrame and converts it into a DoubleArray.
 * @param outputColumn (optional) is the index of the output column.
 * @return the output class column as a DoubleArray.
 */
fun DataFrame.classesAsDoubleArray(outputColumn: Int = lastColumnIndex): DoubleArray =
    this.classes(outputColumn).column("Class").toDoubleArray()

/**
 * Selects the output column from this DataFrame and converts it into an IntArray.
 * @param outputColumn (optional) is the index of the output column.
 * @return the output class column as an IntArray.
 */
fun DataFrame.classesArray(outputColumn: Int = lastColumnIndex): IntArray =
    this.classes(outputColumn).intVector(0).toIntArray()

/**
 * Selects the name of the output column from this DataFrame.
 * @param outputColumn (optional) is the index of the output column.
 * @return this DataFrame output column name.
 */
fun DataFrame.name(outputColumn: Int = lastColumnIndex): String =
    this.column(outputColumn).name()

/**
 * Creates a Map with a Description for each DataFrame column.
 * @return the Map associating column name and description.
 */
val DataFrame.description: Map<String, Description>
    get() = this.inputs().schema()
        .fields()
        .filter { it.isNumeric }
        .associate { it.name to createDescriptionPair(this.column(it.name).toDoubleArray()) }

/**
 * Filter this DataFrame with respect to the output value.
 * @param output is the output value for the matching.
 * @return the filtered DataFrame.
 */
fun DataFrame.filterByOutput(output: Any): DataFrame =
    DataFrame.of(
        stream().filter { it.get(this.lastColumnIndex) == output }
    )

fun DataFrame.clone(): DataFrame = DataFrame.of(this.stream().map { it.clone() })

fun Tuple.clone(): Tuple = Tuple.of(Array(length()) { get(it) }, schema())

fun Tuple.setValue(feature: String, value: Any): Tuple {
    val schema = schema()
    val index = schema.fieldIndex(feature)
    return Tuple.of(Array(length()) { if (it == index) value else get(it) }, schema)
}

/**
 * Writes the supplied value in all elements of the specified column.
 * @param feature is the name of the column to write.
 * @param value is the value to write.
 * @return the modified DataFrame.
 */
fun DataFrame.writeColumn(feature: String, value: Any): DataFrame =
    DataFrame.of(this.stream().map { it.setValue(feature, value) })

private fun DataFrame.initRanges(name: String) =
    this.categories().map {
        val desc = this.filterByOutput(it).description[name]
        Range(desc!!.mean, desc.stdDev)
    }.sortedWith(compareBy { it.mean })

/**
 * Creates a List of Range for the specified feature.
 * @param name is the feature name.
 * @return the List with the sorted ranges.
 */
fun DataFrame.createRanges(name: String): List<Range> {
    val ranges = this.initRanges(name)
    expandRanges(ranges)
    this.description[name].apply {
        ranges.first().leftInfinite()
        ranges.last().rightInfinite()
    }
    return ranges
}

/**
 * Creates a data structure for efficiently discretise this DataFrame.
 * @return the discretisation logic.
 */
fun DataFrame.splitFeatures(): Discretization {
    val features: MutableSet<DiscreteFeature> = mutableSetOf()
    for (feature in this.inputs()) {
        features.add(
            DiscreteFeature(feature.name(), createSet(feature, this))
        )
    }
    return Discretization.Unordered(features.toSet())
}

/**
 * One-hot encodes the numerical and categorical features of this DataFrame.
 * @param featureSets is the encoding strategy.
 * @return the one-hot encoded DataFrame.
 */
fun DataFrame.toBoolean(features: Discretization): DataFrame {
    val outputColumns: MutableList<BaseVector<*, *, *>> = mutableListOf()
    for (column in this.inputs()) {
        val match = features[column.name()]
        if (match == null) {
            outputColumns.add(column)
        } else {
            for ((name, value) in match.admissibleValues) {
                outputColumns.add(createColumn(name, value, column))
            }
        }
    }
    return DataFrame.of(*outputColumns.toTypedArray()).merge(this.outputs())
}

/**
 * Converts this DataFrame into a String List.
 * @return this DataFrame as a List of strings.
 */
fun DataFrame.toStringList(): List<String> =
    this.stream().map { it.toString() }.toList()

/**
 * Converts this DataFrame into a String Set.
 * @return this DataFrame as a Set of strings.
 */
fun DataFrame.toStringSet(): Set<String> =
    this.toStringList().toSet()
