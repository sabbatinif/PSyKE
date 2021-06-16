package it.unibo.skpf.re.regression.iter

import it.unibo.skpf.re.*
import it.unibo.skpf.re.utils.createHead
import it.unibo.skpf.re.utils.createTerm
import it.unibo.skpf.re.utils.createVariableList
import it.unibo.skpf.re.regression.HyperCube
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.theory.MutableTheory
import it.unibo.tuprolog.theory.Theory
import smile.data.*
import smile.data.type.StructType
import smile.regression.Regression
import kotlin.streams.toList
import kotlin.streams.asStream
import it.unibo.skpf.re.OriginalValue.Interval.Between
import javax.management.InvalidAttributeValueException
import kotlin.math.*

class ITER(
    override val predictor: Regression<DoubleArray>,
    override val featureSet: Collection<BooleanFeatureSet> = emptySet(),
    private val minUpdate: Double,
    private var nPoints: Int,
    private var maxIterations: Int,
    private var minExamples: Int,
    private var threshold: Double,
    private var fillGaps: Boolean
) : Extractor<DoubleArray, Regression<DoubleArray>> {

    private lateinit var fakeDataset: DataFrame
    private lateinit var hyperCubes: Collection<HyperCube>

    private fun init(dataset: DataFrame): Triple<MutableList<HyperCube>, List<MinUpdate>, HyperCube> {
        this.fakeDataset = dataset.inputs()
        val surrounding = HyperCube.createSurroundingCube(dataset)
        val minUpdates = calculateMinUpdates(surrounding)
        var hyperCubes: List<HyperCube>
        do {
            hyperCubes = generateStartingPoints(dataset)
            hyperCubes.forEach { hyperCube ->
                hyperCube.expandAll(minUpdates, surrounding)
            }
            nPoints--
        } while (hyperCubes.any { HyperCube.checkOverlap(hyperCubes.toMutableList(), hyperCubes) } )
        hyperCubes.forEach { it.updateMean(dataset.inputs(), predictor) }
        return Triple(hyperCubes.toMutableList(), minUpdates, surrounding)
    }

    override fun extract(dataset: DataFrame): Theory {
        val (hyperCubes, minUpdates, surrounding) = init(dataset)
        val tempTrain = dataset.stream().toList().toMutableList()
        val limits = mutableSetOf<Limit>()
        var iterations = 0
        while (tempTrain.isNotEmpty()) {
            if (iterations >= maxIterations) break
            iterations +=
                iterate(dataset, hyperCubes, surrounding, minUpdates, limits, maxIterations - iterations)
            if (fillGaps)
                throw NotImplementedError()
        }
        this.hyperCubes = hyperCubes
        return createTheory(dataset)
    }

    private fun iterate(
        dataset: DataFrame,
        hyperCubes: MutableList<HyperCube>,
        surrounding: HyperCube,
        minUpdates: Collection<MinUpdate>,
        limits: MutableSet<Limit>,
        leftIterations: Int
    ): Int {
        var iterations = 1
        while (limits.size <= dataset.inputs().ncols() * 2 * hyperCubes.size) {
            if (iterations == leftIterations) break
            cubesToUpdate(dataset, hyperCubes, surrounding, minUpdates, limits)
                .minByOrNull { it.second!!.distance }
                ?.apply {
                    expandOrCreate(first, second!!, hyperCubes)
                }
            iterations++
        }
        return iterations
    }

    private fun expandOrCreate(
        cube: HyperCube,
        expansion: Expansion,
        hyperCubes: MutableList<HyperCube>
    ) {
        if (expansion.distance > threshold)
            hyperCubes.add(expansion.cube)
        else
            cube.expand(expansion, hyperCubes)
    }

    private fun cubesToUpdate(
        dataset: DataFrame,
        hyperCubes: Collection<HyperCube>,
        surrounding: HyperCube,
        minUpdates: Collection<MinUpdate>,
        limits: MutableSet<Limit>
    ) = sequence {
        hyperCubes.forEach {
            yield(it to bestCube(
                dataset,
                it,
                createTempCubes(dataset, limits, it, surrounding, minUpdates, hyperCubes)
            ))
        }
    }.toList().filter { it.second != null }

    private fun bestCube(
        dataset: DataFrame,
        cube: HyperCube,
        cubes: List<Limit>
    ): Expansion? {
        return cubes
            .filter { it.cube.count(dataset) != null }
            .map { limit ->
                val fake = limit.cube.count(dataset)
                    ?.let { createFakeSamples(limit.cube, dataset.inputs().schema(), it) }
                if (fake != null)
                    fakeDataset = fakeDataset.union(fake)
                limit.cube.updateMean(fakeDataset, predictor)
                Expansion(limit.cube, limit.feature, limit.direction, abs(cube.mean - limit.cube.mean))
        }.minByOrNull { it.distance }
    }

    private fun createFakeSamples(
        cube: HyperCube,
        schema: StructType,
        n: Int
    ): DataFrame? {
        val dataset = sequence {
            (n..minExamples).forEach { _ ->
                yield(cube.createTuple(schema))
            }
        }
        return if (dataset.none()) null else DataFrame.of(dataset.asStream())
    }

    private fun createTempCubes(
        dataset: DataFrame,
        limits: MutableSet<Limit>,
        cube: HyperCube,
        surrounding: HyperCube,
        minUpdates: Collection<MinUpdate>,
        hyperCubes: Collection<HyperCube>
    ) = sequence {
        for (feature in dataset.inputs().schema().fields()) {
            val limit = checkLimits(limits, cube, feature.name)
            if (limit == '*') continue
            listOf('-', '+').minus(limit).forEach {
                yieldAll(createTempCube(cube, surrounding, limits, minUpdates, hyperCubes, feature.name, it!!))
            }
        }
    }.toList()

    private fun createTempCube(
        cube: HyperCube,
        surrounding: HyperCube,
        limits: MutableSet<Limit>,
        minUpdates: Collection<MinUpdate>,
        hyperCubes: Collection<HyperCube>,
        feature: String,
        direction: Char
    ) = sequence {
        val (tempCube, values) = createRange(cube, surrounding, minUpdates, feature, direction)
        tempCube.updateDimension(feature, values)
        var overlap = tempCube.overlap(hyperCubes)
        if (overlap != null)
            overlap = resolveOverlap(tempCube, overlap, hyperCubes, feature, direction)
        if ((tempCube.hasVolume()) && (overlap == null) && (!tempCube.equal(hyperCubes)))
            yield(Limit(tempCube, feature, direction))
        else
            limits.add(Limit(cube, feature, direction))
    }.toList()

    private fun resolveOverlap(
        cube: HyperCube,
        overlappingCube: HyperCube,
        hyperCubes: Collection<HyperCube>,
        feature: String,
        direction: Char
    ): HyperCube? {
        val (a, b) = cube.get(feature)
        cube.updateDimension(
            feature,
            if (direction == '-') max(overlappingCube.getSecond(feature), a) else a,
            if (direction == '+') min(overlappingCube.getFirst(feature), b) else b
        )
        return cube.overlap(hyperCubes)
    }

    private fun createRange(
        cube: HyperCube,
        surrounding: HyperCube,
        minUpdates: Collection<MinUpdate>,
        feature: String,
        direction: Char
    ): Pair<HyperCube, Pair<Double, Double>> {
        val (a, b) = cube.get(feature)
        val size = minUpdates.first { it.name == feature }.value
        return cube.copy() to
                if (direction == '-')
                    max(a - size, surrounding.getFirst(feature)) to a
                else
                    b to min(b + size, surrounding.getSecond(feature))
    }

    private fun checkLimits(limits: MutableSet<Limit>, cube: HyperCube, feature: String): Char? {
        var res: Char? = null
        for (limit in limits) {
            if ((limit.cube == cube) && (limit.feature == feature)) {
                if (res == null)
                    res = limit.direction
                else
                    return '*'
            }
        }
        return res
    }

    private fun calculateMinUpdates(surrounding: HyperCube) =
        surrounding.dimensions.map { (name, interval) ->
            MinUpdate(name, (interval.second - interval.first) * minUpdate)
        }

    private fun generateStartingPoints(dataset: DataFrame): List<HyperCube> {
        val min = dataset.outputsArray().minByOrNull { it }!!
        val max = dataset.outputsArray().maxByOrNull { it }!!
        val points = when (nPoints) {
            in Int.MIN_VALUE..0 -> throw InvalidAttributeValueException(nPoints.toString())
            1 -> listOf((max - min) / 2)
            else -> (1..nPoints).map { min + (max - min) / (nPoints - 1) * (it - 1) }
        }
        return points.map { findCloserSample(dataset, it) }.map { HyperCube.cubeFromPoint(it) }
    }

    private fun findCloserSample(dataset: DataFrame, output: Double): Tuple {
        val index = dataset.outputsArray().withIndex()
            .minByOrNull { (_, value) ->
                abs(value - output)
            }!!.index
        return dataset[index]
    }

    private fun createTheory(dataset: DataFrame): Theory {
        val variables = createVariableList(emptySet(), dataset)
        val theory = MutableTheory.empty()
        for (cube in hyperCubes)
            theory.assertZ(
                Clause.of(
                    createHead(dataset.name(), variables.values, cube.mean),
                    *createBody(variables, cube.dimensions)
                ))
        return theory
    }

    private fun createBody(
        variables: Map<String, Var>,
        dimensions: Map<String, Pair<Double, Double>>
    ) = sequence {
        for ((name, values) in dimensions)
            yield(createTerm(variables[name], Between(values.first, values.second)))
    }.toList().toTypedArray()

    private fun predict(tuple: Tuple): Double {
        this.hyperCubes.firstOrNull {
            it.contains(tuple)
        }.apply {
            return this?.mean ?: Double.NaN
        }
    }

    override fun predict(dataset: DataFrame): Array<*> =
        dataset.inputs().stream().map { this.predict(it) }.toList().toTypedArray()
}