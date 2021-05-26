package smile.data

import it.unibo.skpf.re.BooleanFeatureSet
import it.unibo.skpf.re.OriginalValue
import it.unibo.skpf.re.OriginalValue.Interval
import it.unibo.skpf.re.OriginalValue.Value
import smile.data.type.DataTypes
import smile.data.type.StructField
import smile.data.type.StructType
import smile.data.vector.BaseVector
import smile.data.vector.DoubleVector
import smile.data.vector.IntVector
import smile.data.vector.StringVector
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.streams.toList

fun DataFrame.randomSplit(percent: Double, seed: Long = 10L): Pair<DataFrame, DataFrame> {
    val r1 = Random(seed)
    val r2 = Random(seed)
    val train = DataFrame.of(this.stream().toList().filter { r1.nextDouble() >= percent })
    val test = DataFrame.of(this.stream().toList().filter { r2.nextDouble() < percent })
    return train to test
}

val DataFrame.lastColumnIndex: Int
    get() = this.ncols() - 1

fun DataFrame.inputs(outputColumn: Int = lastColumnIndex): DataFrame =
    this.drop(outputColumn)

fun DataFrame.categories(i: Int = lastColumnIndex): Set<Any> =
    this.outputClasses(i).distinct().asSequence().toSet()

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
    this.classes(outputColumn).column("Class").toDoubleArray()

fun DataFrame.classesArray(outputColumn: Int = lastColumnIndex): IntArray =
    this.classes(outputColumn).intVector(0).toIntArray()

fun DataFrame.describe(): Map<String, Description> {
    fun std(col: DoubleArray): Double {
        val mean = col.average()
        var std = 0.0
        col.forEach { std += (it - mean).pow(2.0) }
        return sqrt(std / col.size)
    }

    return mapOf(*this.inputs().schema().fields()
        .filter { it.isNumeric }
        .map {
            val col = this.column(it.name).toDoubleArray()
            it.name to Description(
                col.average(),
                std(col),
                col.min() ?: 0.0,
                col.max() ?: 1.0
            )
        }.toTypedArray()
    )
}

fun DataFrame.filterByOutput(output: Any): DataFrame {
    return DataFrame.of(this.stream().filter {
        it.get(this.lastColumnIndex) == output
    })
}

fun DataFrame.writeColumn(feature: String, value: Any): DataFrame {
    return DataFrame.of(*this.map {
        if (it.name() == feature) {
            when (value) {
                is Double -> DoubleVector.of(this.schema().field(feature),
                    DoubleArray(this.nrows()) { value }
                )
                is Int -> IntVector.of(this.schema().field(feature),
                    IntArray(this.nrows()) { value }
                )
                is String -> StringVector.of(this.schema().field(feature),
                    *Array<String>(this.nrows()) { value }
                )
                else -> throw IllegalStateException()
            }
        } else {
            it
        }
    }.toTypedArray())
}

fun DataFrame.createRanges(name: String): List<Range> {
    val ranges = this.categories().map {
        val desc = this.filterByOutput(it).describe()[name]
        Range(desc!!.mean, desc.stdDev)
    }.sortedWith(compareBy { it.mean })

    ranges.zipWithNext { r1, r2 ->
        while (r1.upper < r2.lower) {
            r1.upper += r1.std
            r2.lower -= r2.std
        }
        val mean = (r1.upper - r1.std + r2.lower + r2.std) / 2
        r1.upper = mean
        r2.lower = mean
    }
    this.describe()[name].apply {
        ranges.first().lower = this!!.min - 0.001
        ranges.last().upper = this.max + 0.001
    }
    return ranges
}

fun DataFrame.splitFeatures(): Set<BooleanFeatureSet> {
    //fun Double.format(digits: Int) = "%.${digits}f".format(this)
    val featureSets: MutableSet<BooleanFeatureSet> = mutableSetOf()
    for (feature in this.inputs()) {
        when (feature.type()) {
            DataTypes.DoubleType ->
                featureSets.add(
                    BooleanFeatureSet(
                        feature.name(),
                        createRanges(feature.name()).mapIndexed { i, it ->
                            "${feature.name()}_$i" to Interval(it.lower, it.upper)
                        }.toMap()
                    )
                )
            DataTypes.StringType ->
                featureSets.add(
                    BooleanFeatureSet(
                        feature.name(),
                        feature.toStringArray().distinct().mapIndexed { i, it ->
                            "${feature.name()}_$i" to Value(it)
                        }.toMap()
                    )
                )
            DataTypes.IntegerType ->
                featureSets.add(
                    BooleanFeatureSet(
                        feature.name(),
                        feature.toIntArray().distinct().mapIndexed { i, it ->
                            "${feature.name()}_$i" to Value(it)
                        }.toMap()
                    )
                )
        }
    }
    return featureSets.toSet()
}

fun DataFrame.toBoolean(featureSets: Set<BooleanFeatureSet>): DataFrame {
    val outputColumns: MutableList<BaseVector<*, *, *>> = mutableListOf()
    for (column in this.inputs()) {
        val match = featureSets.filter { it.name == column.name() }
        if (match.isEmpty())
            outputColumns.add(column)
        else
            for ((name, value) in match.first().set)
                outputColumns.add(createColumn(name, value, column))
    }
    return DataFrame.of(*outputColumns.toTypedArray()).merge(this.outputs())
}

private fun createColumn(name: String, value: OriginalValue, column: BaseVector<*, *, *>): DoubleVector {
    fun condition(original: OriginalValue, value: Any): Boolean {
        return if ((original is Interval) && (value is Double))
            (original.lower <= value) && (value < original.upper)
        else if ((original is Value) && (value is String))
            (original.value == value)
        else
            throw IllegalStateException()
    }

    return DoubleVector.of(
        StructField(name, DataTypes.DoubleType),
        when (value) {
            is Interval -> column.toDoubleArray().toTypedArray()
            is Value -> column.toStringArray().toList().toTypedArray()
        }.map { if (condition(value, it)) 1.0 else 0.0 }.toDoubleArray()
    )
}

fun DataFrame.toStringList(): List<String> {
    return this.stream().toList().map { it.toString() }
}

fun DataFrame.toStringSet(): Set<String> {
    return this.toStringList().toSet()
}

