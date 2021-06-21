package it.unibo.skpf.re.regression

import it.unibo.skpf.re.regression.iter.Expansion
import it.unibo.skpf.re.regression.iter.MinUpdate
import it.unibo.skpf.re.regression.iter.ZippedDimension
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.description
import smile.data.type.StructType
import smile.regression.Regression
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.streams.toList

internal class HyperCube(
    private val dimension: MutableMap<String, Pair<Double, Double>> = mutableMapOf(),
    private var output: Double
) {

    val dimensions: Map<String, Pair<Double, Double>>
        get() = dimension

    val mean: Double
        get() = output

    fun get(feature: String): Pair<Double, Double> =
        dimension[feature] ?: throw FeatureNotFoundException(feature)

    fun getFirst(feature: String): Double =
        this.get(feature).first

    fun getSecond(feature: String): Double =
        this.get(feature).second

    fun copy(): HyperCube {
        return HyperCube(dimensions.toMutableMap(), mean)
    }

    fun expand(
        expansion: Expansion,
        hypercubes: Collection<HyperCube>
    ) {
        expansion.apply {
            val (a, b) = get(feature)
            dimension[feature] = if (direction == '-') Pair(values.first, b) else Pair(a, values.second)
            overlap(hypercubes).apply {
                if (this != null) {
                    this@HyperCube.dimension[feature] =
                        if (direction == '-') Pair(getSecond(feature), b) else Pair(a, getFirst(feature))
                }
            }
        }
    }

    fun expandAll(
        updates: Collection<MinUpdate>,
        surrounding: HyperCube
    ) {
        updates.forEach { this.expandOne(it, surrounding) }
    }

    private fun expandOne(
        update: MinUpdate,
        surrounding: HyperCube
    ) {
        this.dimension[update.name] =
            max(getFirst(update.name) - update.value, surrounding.getFirst(update.name)) to
                min(getSecond(update.name) + update.value, surrounding.getSecond(update.name))
    }

    private fun zipDimensions(cube: HyperCube) = sequence {
        dimension.keys.forEach {
            yield(ZippedDimension(it, get(it), cube.get(it)))
        }
    }

    fun overlap(cube: HyperCube): Boolean {
        zipDimensions(cube).forEach {
            if ((it.otherCube.first >= it.thisCube.second) ||
                (it.thisCube.first >= it.otherCube.second)
            )
                return false
        }
        return true
    }

    fun overlap(hyperCubes: Collection<HyperCube>): HyperCube? {
        hyperCubes.forEach {
            if (this != it && this.overlap(it))
                return it
        }
        return null
    }

    private fun equal(cube: HyperCube): Boolean =
        zipDimensions(cube).all {
            (abs(it.thisCube.first - it.otherCube.first) < epsilon) &&
                (abs(it.thisCube.second - it.otherCube.second) < epsilon)
        }

    fun hasVolume(): Boolean =
        this.dimension.all { (_, values) ->
            values.second - values.first > epsilon
        }

    fun equal(cubes: Collection<HyperCube>): Boolean {
        cubes.forEach {
            if (this.equal(it))
                return true
        }
        return false
    }

    fun contains(tuple: Tuple) =
        tuple.schema().fields().map { it.name!! }.all {
            val value = tuple.getDouble(it)
            (getFirst(it) <= value) && (value < getSecond(it))
        }

    private fun filterDataFrame(dataset: DataFrame): DataFrame? {
        val data = dataset.stream().filter { tuple ->
            dimension.all { (name, values) ->
                (values.first <= tuple.getDouble(name)) &&
                    (tuple.getDouble(name) < values.second)
            }
        }.toList()
        return if (data.isNotEmpty()) DataFrame.of(data) else null
    }

    fun count(dataset: DataFrame) =
        this.filterDataFrame(dataset)?.nrows()

    fun createTuple(schema: StructType): Tuple {
        return Tuple.of(
            dimension.map { (_, values) ->
                Random.nextDouble(values.first, values.second)
            }.toDoubleArray(),
            schema
        )
    }

    fun updateMean(dataset: DataFrame, predictor: Regression<DoubleArray>) {
        val filtered = filterDataFrame(dataset)
        this.output = predictor.predict(filtered?.toArray()).average()
    }

    fun updateDimension(feature: String, values: Pair<Double, Double>) {
        this.dimension[feature] = values
    }

    fun updateDimension(feature: String, lower: Double, upper: Double) =
        updateDimension(feature, Pair(lower, upper))

    companion object {
        const val epsilon = 1.0 / 1000

        @JvmStatic
        fun createSurroundingCube(dataset: DataFrame) =
            HyperCube(
                dataset.description.map { (name, description) ->
                    name to Pair(floor(description.min), ceil(description.max))
                }.toMap() as MutableMap<String, Pair<Double, Double>>,
                0.0
            )

        @JvmStatic
        fun cubeFromPoint(point: Tuple) =
            point.schema().let { schema ->
                HyperCube(
                    (0 until schema.length() - 1).associate {
                        schema.fields()[it].name to Pair(point.getDouble(it), point.getDouble(it))
                    } as MutableMap<String, Pair<Double, Double>>,
                    point.getDouble(schema.fieldName(schema.length() - 1))
                )
            }

        @JvmStatic
        fun checkOverlap(
            toCheck: MutableList<HyperCube>,
            hypercubes: Collection<HyperCube>
        ): Boolean {
            val checked = mutableListOf<HyperCube>()
            while (toCheck.isNotEmpty()) {
                val cube = toCheck.removeAt(0)
                checked.add(cube)
                hypercubes.forEach {
                    if (!checked.contains(it) && cube.overlap(it))
                        return true
                }
            }
            return false
        }
    }
}
