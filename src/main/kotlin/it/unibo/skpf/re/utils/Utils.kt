package it.unibo.skpf.re.utils

import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.OriginalValue
import smile.data.DataFrame
import smile.data.Description
import smile.data.Range
import smile.data.createRanges
import smile.data.type.DataTypes
import smile.data.type.StructField
import smile.data.vector.BaseVector
import smile.data.vector.DoubleVector
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.math.pow
import kotlin.math.sqrt

internal fun saveToFile(filename: String, item: Any) {
    val file = File("src").resolve("test").resolve("resources").resolve(filename)
    return ObjectOutputStream(FileOutputStream(file)).use {
        it.writeObject(item)
    }
}

internal fun loadFromFile(filename: String): Any {
    val file = Extractor::class.java.getResourceAsStream("/$filename")!!
    return ObjectInputStream(file).use {
        it.readObject()
    }
}

internal fun Double.round(digits: Int = 2): Double {
    val k = 10.0.pow(digits)
    return kotlin.math.round(this * k) / k
}

internal inline fun <reified C, R> C.getFieldValue(name: String): R {
    val valueField = C::class.java.getDeclaredField(name)
    valueField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return valueField.get(this) as R
}

internal fun std(col: DoubleArray): Double {
    val mean = col.average()
    var std = 0.0
    col.forEach { std += (it - mean).pow(2.0) }
    return sqrt(std / col.size)
}

internal fun createOriginalValue(value: Any): OriginalValue {
    return if (value is Range)
        when {
            value.lower == Double.NEGATIVE_INFINITY -> OriginalValue.Interval.LessThan(value.upper)
            value.upper == Double.POSITIVE_INFINITY -> OriginalValue.Interval.GreaterThan(value.lower)
            else -> OriginalValue.Interval.Between(value.lower, value.upper)
        }
    else
        OriginalValue.Value(value)
}

internal fun createSet(feature: BaseVector<*, *, *>, dataset: DataFrame) =
    when (feature.type()) {
        DataTypes.DoubleType ->
            dataset.createRanges(feature.name())
        DataTypes.StringType ->
            feature.toStringArray().distinct()
        DataTypes.IntegerType ->
            feature.toIntArray().distinct()
        else -> throw TypeNotAllowedException(feature.type().javaClass.toString())
    }.mapIndexed { i, it -> "${feature.name()}_$i" to createOriginalValue(it) }.toMap()

internal fun expandRanges(ranges: List<Range>) {
    ranges.zipWithNext { r1, r2 ->
        while (r1.upper < r2.lower) {
            r1.expandRight()
            r2.expandLeft()
        }
        val mean = ((r1.upper - r1.std + r2.lower + r2.std) / 2).round(2)
        r1.upper = mean
        r2.lower = mean
    }
}

internal fun condition(original: OriginalValue, value: Any) =
    if ((original is OriginalValue.Interval) && (value is Double))
        (original.lower <= value) && (value < original.upper)
    else if ((original is OriginalValue.Value) && (value is String))
        (original.value == value)
    else
        throw IllegalArgumentException(
            "Can only associate Interval to Double and Value to String\n" +
                "Actual types are " + original.javaClass + " and " + value.javaClass
        )

internal fun createColumn(name: String, value: OriginalValue, column: BaseVector<*, *, *>) =
    DoubleVector.of(
        StructField(name, DataTypes.DoubleType),
        when (value) {
            is OriginalValue.Interval -> column.toDoubleArray().toTypedArray()
            is OriginalValue.Value -> column.toStringArray().toList().toTypedArray()
        }.map { if (condition(value, it)) 1.0 else 0.0 }.toDoubleArray()
    )

/**
 * Creates a Description of the column passed as input.
 * @param column is the column to describe.
 * @return the column description.
 */
internal fun createDescriptionPair(column: DoubleArray): Description {
    return Description(
        column.average(),
        std(column),
        column.minByOrNull { it } ?: 0.0,
        column.maxByOrNull { it } ?: 1.0
    )
}
