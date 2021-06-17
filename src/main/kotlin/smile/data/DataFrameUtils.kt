package smile.data

import it.unibo.skpf.re.schema.Feature
import it.unibo.skpf.re.schema.Value
import it.unibo.skpf.re.schema.Value.Interval
import it.unibo.skpf.re.schema.Value.Constant
import it.unibo.skpf.re.utils.TypeNotAllowedException
import it.unibo.skpf.re.utils.round
import smile.data.type.DataTypes
import smile.data.type.StructField
import smile.data.type.StructType
import smile.data.vector.BaseVector
import smile.data.vector.DoubleVector
import smile.data.vector.IntVector
import smile.data.vector.StringVector
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.streams.toList

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

val DataFrame.lastColumnIndex: Int
    get() = this.ncols() - 1

fun DataFrame.inputs(outputColumn: Int = lastColumnIndex): DataFrame =
    this.drop(outputColumn)

fun DataFrame.categories(i: Int = lastColumnIndex): Set<Any> =
    this.outputClasses(i).distinct().toSet()

fun DataFrame.nCategories(i: Int = lastColumnIndex): Int =
    this.categories(i).size

fun DataFrame.outputClasses(i: Int = lastColumnIndex): List<Any> =
    this.stream().map { it[i] }.toList()

fun DataFrame.classes(outputColumn: Int = lastColumnIndex): DataFrame {
    val classes = categories(outputColumn)
    return DataFrame.of(
        this.select(outputColumn).stream().map {
            val klass = classes.indexOf(it[0])
            if (klass < 0) throw IllegalStateException("Invalid class: ${it[0]}")
            Tuple.of(arrayOf(klass), StructType(StructField("Class", DataTypes.IntegerType)))
        }
    )
}

fun DataFrame.outputs(outputColumn: Int = lastColumnIndex): DataFrame =
    this.select(outputColumn)

fun DataFrame.inputsArray(outputColumn: Int = lastColumnIndex): Array<DoubleArray> =
    this.inputs(outputColumn).toArray()

fun DataFrame.outputsArray(outputColumn: Int = lastColumnIndex): DoubleArray =
    this.column(outputColumn).toDoubleArray()

fun DataFrame.classesAsDoubleArray(outputColumn: Int = lastColumnIndex): DoubleArray =
    this.classes(outputColumn).column("Class").toDoubleArray()

fun DataFrame.classesArray(outputColumn: Int = lastColumnIndex): IntArray =
    this.classes(outputColumn).intVector(0).toIntArray()

fun DataFrame.name(outputColumn: Int = lastColumnIndex): String =
    this.column(outputColumn).name()

fun std(col: DoubleArray): Double {
    val mean = col.average()
    var std = 0.0
    col.forEach { std += (it - mean).pow(2.0) }
    return sqrt(std / col.size)
}

fun createDescriptionPair(name: String, column: DoubleArray): Pair<String, Description> {
    return name to Description(
        column.average(),
        std(column),
        column.minByOrNull { it } ?: 0.0,
        column.maxByOrNull { it } ?: 1.0
    )
}

fun DataFrame.describe(): Map<String, Description> =
    mapOf(
        *this.inputs().schema().fields()
            .filter { it.isNumeric }
            .map { field ->
                createDescriptionPair(field.name, this.column(field.name).toDoubleArray())
            }.toTypedArray()
    )

fun DataFrame.filterByOutput(output: Any): DataFrame =
    DataFrame.of(
        this.stream().filter {
            it.get(this.lastColumnIndex) == output
        }
    )

private fun createFakeColumn(value: Any, n: Int, field: StructField) =
    when (value) {
        is Double -> DoubleVector.of(field, DoubleArray(n) { value })
        is Int -> IntVector.of(field, IntArray(n) { value })
        is String -> StringVector.of(field, *Array(n) { value })
        else -> throw TypeNotAllowedException(value.javaClass.toString())
    }

fun DataFrame.writeColumn(feature: String, value: Any): DataFrame =
    DataFrame.of(
        *this.map {
            if (it.name() == feature)
                createFakeColumn(value, this.nrows(), this.schema().field(feature))
            else
                it
        }.toTypedArray()
    )

fun DataFrame.initRanges(name: String) =
    this.categories().map {
        val desc = this.filterByOutput(it).describe()[name]
        Range(desc!!.mean, desc.stdDev)
    }.sortedWith(compareBy { it.mean })

private fun expandRanges(ranges: List<Range>) {
    ranges.zipWithNext { r1, r2 ->
        while (r1.upper < r2.lower) {
            r1.upper += r1.std
            r2.lower -= r2.std
        }
        val mean = ((r1.upper - r1.std + r2.lower + r2.std) / 2).round(2)
        r1.upper = mean
        r2.lower = mean
    }
}

fun DataFrame.createRanges(name: String): List<Range> {
    val ranges = this.initRanges(name)
    expandRanges(ranges)
    this.describe()[name].apply {
        ranges.first().openLower()
        ranges.last().openUpper()
    }
    return ranges
}

private fun createSet(feature: BaseVector<*, *, *>, dataset: DataFrame) =
    when (feature.type()) {
        DataTypes.DoubleType ->
            dataset.createRanges(feature.name())
        DataTypes.StringType ->
            feature.toStringArray().distinct()
        DataTypes.IntegerType ->
            feature.toIntArray().distinct()
        else -> throw TypeNotAllowedException(feature.type().javaClass.toString())
    }.mapIndexed { i, it -> "${feature.name()}_$i" to createOriginalValue(it) }.toMap()

fun createOriginalValue(originalValue: Any): Value {
    return if (originalValue is Range)
        when {
            originalValue.lower == NEGATIVE_INFINITY -> Interval.LessThan(originalValue.upper)
            originalValue.upper == POSITIVE_INFINITY -> Interval.GreaterThan(originalValue.lower)
            else -> Interval.Between(originalValue.lower, originalValue.upper)
        }
    else
        Constant(originalValue)
}

fun DataFrame.splitFeatures(): Set<Feature> {
    val features: MutableSet<Feature> = mutableSetOf()
    for (feature in this.inputs())
        features.add(
            Feature(feature.name(), createSet(feature, this))
        )
    return features.toSet()
}

fun DataFrame.toBoolean(features: Set<Feature>): DataFrame {
    val outputColumns: MutableList<BaseVector<*, *, *>> = mutableListOf()
    for (column in this.inputs()) {
        val match = features.filter { it.name == column.name() }
        if (match.isEmpty())
            outputColumns.add(column)
        else
            for ((name, value) in match.first().admissibleValues)
                outputColumns.add(createColumn(name, value, column))
    }
    return DataFrame.of(*outputColumns.toTypedArray()).merge(this.outputs())
}

private fun condition(original: Value, value: Any) =
    if ((original is Interval) && (value is Double))
        (original.lower <= value) && (value < original.upper)
    else if ((original is Constant) && (value is String))
        (original.value == value)
    else
        throw IllegalArgumentException(
            "Can only associate Interval to Double and Value to String\n" +
                "Actual types are " + original.javaClass + " and " + value.javaClass
        )

private fun createColumn(name: String, value: Value, column: BaseVector<*, *, *>) =
    DoubleVector.of(
        StructField(name, DataTypes.DoubleType),
        when (value) {
            is Interval -> column.toDoubleArray().toTypedArray()
            is Constant -> column.toStringArray().toList().toTypedArray()
        }.map { if (condition(value, it)) 1.0 else 0.0 }.toDoubleArray()
    )

fun DataFrame.toStringList(): List<String> =
    this.stream().toList().map { it.toString() }

fun DataFrame.toStringSet(): Set<String> =
    this.toStringList().toSet()
