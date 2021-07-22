package it.unibo.skpf.re.regression.iter

import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.regression.HyperCube
import it.unibo.skpf.re.schema.Discretization
import it.unibo.skpf.re.schema.Value.Interval.Between
import it.unibo.skpf.re.utils.createHead
import it.unibo.skpf.re.utils.createTerm
import it.unibo.skpf.re.utils.createVariableList
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Var
import it.unibo.tuprolog.theory.MutableTheory
import it.unibo.tuprolog.theory.Theory
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.inputs
import smile.data.name
import smile.data.outputsArray
import smile.data.type.StructType
import smile.regression.Regression
import javax.management.InvalidAttributeValueException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.streams.asStream
import kotlin.streams.toList

internal typealias DomainProperties = Pair<List<MinUpdate>, HyperCube>

internal class ITER(
    override val predictor: Regression<DoubleArray>,
    override val discretization: Discretization = Discretization.Empty,
    private val minUpdate: Double,
    private var nPoints: Int,
    private var maxIterations: Int,
    private var minExamples: Int,
    private var threshold: Double,
    private var fillGaps: Boolean,
    private var seed: Long = 123L
) : Extractor<DoubleArray, Regression<DoubleArray>> {

    private lateinit var fakeDataset: DataFrame
    private lateinit var hyperCubes: Collection<HyperCube>
    private val random = Random(seed)

    private fun init(dataset: DataFrame): Pair<MutableList<HyperCube>, DomainProperties> {
        this.fakeDataset = dataset.inputs()
        val surrounding = HyperCube.createSurroundingCube(dataset)
        val minUpdates = calculateMinUpdates(surrounding)
        val hyperCubes = initHyperCubes(dataset, minUpdates, surrounding)
        hyperCubes.forEach { it.updateMean(dataset.inputs(), predictor) }
        return hyperCubes.toMutableList() to (minUpdates to surrounding)
    }

    private fun initHyperCubes(
        dataset: DataFrame,
        minUpdates: Collection<MinUpdate>,
        surrounding: HyperCube
    ): List<HyperCube> {
        var hyperCubes: List<HyperCube>
        do {
            hyperCubes = generateStartingPoints(dataset)
            for (hyperCube in hyperCubes)
                hyperCube.expandAll(minUpdates, surrounding)
            nPoints--
        } while (HyperCube.checkOverlap(hyperCubes.toMutableList(), hyperCubes))
        return hyperCubes
    }

    override fun extract(dataset: DataFrame): Theory {
        val (hyperCubes, domain) = init(dataset)
        val tempTrain = dataset.stream().toList().toMutableList()
        var iterations = 0
        while (tempTrain.isNotEmpty()) {
            if (iterations >= maxIterations) break
            iterations +=
                iterate(dataset, hyperCubes, domain, maxIterations - iterations)
            if (fillGaps)
                throw NotImplementedError()
        }
        this.hyperCubes = hyperCubes
        return createTheory(dataset)
    }

    private fun iterate(
        dataset: DataFrame,
        hyperCubes: MutableList<HyperCube>,
        domain: DomainProperties,
        leftIterations: Int
    ): Int {
        var iterations = 1
        while (dataset.inputs().ncols() * 2 * hyperCubes.size >= hyperCubes.fold(0) {
            acc, cube ->
            acc + cube.limitCount
        }
        ) {
            if (iterations == leftIterations) break
            cubesToUpdate(dataset, hyperCubes, domain)
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
        domain: DomainProperties,
    ) = sequence {
        hyperCubes.forEach {
            yield(
                it to bestCube(
                    dataset,
                    it,
                    createTempCubes(dataset, it, domain, hyperCubes)
                )
            )
        }
    }.toList().filter { it.second != null }

    private fun bestCube(
        dataset: DataFrame,
        cube: HyperCube,
        cubes: List<Expansion>
    ): Expansion? {
        return cubes
            .filter { it.cube.count(dataset) != null }
            .map { limit ->
                val fake = limit.cube.count(dataset)
                    ?.let { limit.cube.createFakeSamples(
                        dataset.inputs().schema(), it, minExamples, random
                    ) }
                if (fake != null)
                    fakeDataset = fakeDataset.union(fake)
                limit.cube.updateMean(fakeDataset, predictor)
                Expansion(limit.cube, limit.feature, limit.direction, abs(cube.mean - limit.cube.mean))
            }.minByOrNull { it.distance }
    }

    private fun createTempCubes(
        dataset: DataFrame,
        cube: HyperCube,
        domain: DomainProperties,
        hyperCubes: Collection<HyperCube>
    ) = sequence {
        for (feature in dataset.inputs().schema().fields()) {
            val limit = cube.checkLimits(feature.name)
            if (limit == '*') continue
            listOf('-', '+').minus(limit).forEach {
                yieldAll(createTempCube(cube, domain, hyperCubes, feature.name, it!!))
            }
        }
    }.toList()

    private fun createTempCube(
        cube: HyperCube,
        domain: DomainProperties,
        hyperCubes: Collection<HyperCube>,
        feature: String,
        direction: Char
    ) = sequence {
        val (tempCube, values) = createRange(cube, domain, feature, direction)
        tempCube.updateDimension(feature, values)
        var overlap = tempCube.overlap(hyperCubes)
        if (overlap != null)
            overlap = resolveOverlap(tempCube, overlap, hyperCubes, feature, direction)
        if ((tempCube.hasVolume()) && (overlap == null) && (!tempCube.equal(hyperCubes)))
            yield(Expansion(tempCube, feature, direction))
        else
            cube.addLimit(feature, direction)
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
        domain: DomainProperties,
        feature: String,
        direction: Char
    ): Pair<HyperCube, Pair<Double, Double>> {
        val (minUpdates, surrounding) = domain
        val (a, b) = cube.get(feature)
        val size = minUpdates.first { it.name == feature }.value
        return cube.copy() to
            if (direction == '-')
                max(a - size, surrounding.getFirst(feature)) to a
            else
                b to min(b + size, surrounding.getSecond(feature))
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
        val theory = MutableTheory.empty()
        for (cube in hyperCubes) {
            val variables = createVariableList(emptySet(), dataset)
            theory.assertZ(
                Clause.of(
                    createHead(dataset.name(), variables.values, cube.mean),
                    *createBody(variables, cube.dimensions)
                )
            )
        }
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

    override fun predict(dataset: DataFrame): Array<Double> =
        dataset.inputs().stream().map { this.predict(it) }.toList().toTypedArray()
}
