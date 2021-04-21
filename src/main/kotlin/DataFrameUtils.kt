import smile.data.DataFrame
import smile.data.Tuple
import smile.data.type.DataTypes
import smile.data.type.StructField
import smile.data.type.StructType
import smile.data.vector.DoubleVector
import smile.feature.Normalizer
import kotlin.random.Random
import kotlin.streams.asSequence

data class Description(
    val mean: Double,
    val std: Double,
    val min: Double,
    val max: Double
)

fun DataFrame.randomSplit(percent: Double, seed: Int = 0): Pair<DataFrame, DataFrame> {
    val r1 = Random(seed)
    val r2 = Random(seed)
    val test = DataFrame.of(this.stream().filter { r1.nextDouble() < percent })
    val train = DataFrame.of(this.stream().filter { r2.nextDouble() >= percent })
    return train to test
}

fun DataFrame.normalize(): DataFrame {
    val n = Normalizer(Normalizer.Norm.L2)
    return n.transform(this)
}

val DataFrame.lastColumnIndex: Int
    get() = this.ncols() - 1

fun DataFrame.inputs(outputColumn: Int = lastColumnIndex): DataFrame =
    this.drop(outputColumn)

fun DataFrame.categories(i: Int = lastColumnIndex): Set<Any> =
    this.stream().map { it[i] }.distinct().asSequence().toSet()

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
    this.apply(outputColumn).toArray()

fun DataFrame.classesArray(outputColumn: Int = lastColumnIndex): IntArray =
    this.classes(outputColumn).intVector(0).toIntArray()

fun DataFrame.describe(): Map<String, Description> {
    fun std(col: DoubleArray): Double {
        val mean = col.average()
        var std = 0.0
        col.forEach { std += Math.pow(it - mean, 2.0) }
        return Math.sqrt(std / col.size)
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
        }.toTypedArray() )
}

fun DataFrame.filterByOutput(output: Any): DataFrame {
    return DataFrame.of(this.stream().filter {
        it.get(this.lastColumnIndex) == output
    })
}

fun DataFrame.writeColumn(feature: String, value: Double): DataFrame {
    return DataFrame.of(*this.map {
        if (it.name() == feature)
            DoubleVector.of(this.schema().field(feature),
                DoubleArray(this.nrows()) { value }
            )
        else
            it
    }.toTypedArray() )
}
/*
fun DataFrame.columnToTuple(index: Int): Tuple {
    val col = this.column(index)
    return Tuple.of(arrayOf(col), StructType(StructField(col.name(), col.type())))
}
private fun toOneHot(classes: Array<Int>): Array<DoubleArray> {
    val oneHot = Array(classes.size) { DoubleArray(classes.distinct().size) {0.0} }
    classes.forEachIndexed { i, it -> oneHot[i][it] = 1.0 }
    return oneHot
}
*/