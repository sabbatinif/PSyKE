package it.unibo.skpf.re.regression.gridex

import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.regression.HyperCube
import it.unibo.skpf.re.schema.Discretization
import it.unibo.skpf.re.schema.Value
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
import smile.regression.Regression
import kotlin.random.Random
import kotlin.streams.toList

internal typealias SplitFeature = Map<String, List<Pair<Double, Double>>>
internal typealias TempCube = Map<String, Pair<Double, Double>>
internal typealias SplitEntry = Map.Entry<String, List<Pair<Double, Double>>>

internal class GridEx(
    override val predictor: Regression<DoubleArray>,
    override val discretization: Discretization = Discretization.Empty,
    private val steps: Collection<Int>,
    private val threshold: Double,
    private var minExamples: Int,
    private var seed: Long = 123L
) : Extractor<DoubleArray, Regression<DoubleArray>> {

    private lateinit var fakeDataset: DataFrame
    private lateinit var hyperCubes: Collection<HyperCube>
    private val random = Random(seed)

    override fun extract(dataset: DataFrame): Theory {
        this.fakeDataset = dataset.inputs()
        val adaptive = adaptiveSplits()
        val surrounding = HyperCube.createSurroundingCube(dataset)
        var hypercubes = listOf(surrounding)
        for (step in steps)
        {
            hypercubes = expand(hypercubes, dataset, step, adaptive)
            hypercubes = update(hypercubes, dataset)
            this.hyperCubes = hypercubes
            //merge
        }
        return createTheory(dataset, hypercubes)
    }

    private fun createTheory(dataset: DataFrame, hypercubes: Collection<HyperCube>): Theory {
        val theory = MutableTheory.empty()
        for (cube in hypercubes) {
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
            yield(createTerm(variables[name], Value.Interval.Between(values.first, values.second)))
    }.toList().toTypedArray()

    private fun update(
        hypercubes: List<HyperCube>,
        dataset: DataFrame
    ): List<HyperCube> {
        val newCubes = mutableListOf<HyperCube>()
        for (cube in hypercubes) {
            when (val count = cube.count(dataset)!!) {
                0 -> continue
                in 1..minExamples -> {
                    val fake = cube.createFakeSamples(
                        dataset.inputs().schema(), count, minExamples, random
                    )
                    if (fake != null)
                        fakeDataset = fakeDataset.union(fake)
                }
            }
            cube.updateMeanAndStd(fakeDataset, predictor)
            newCubes.add(cube)
        }
        return newCubes
    }

    private fun expand(
        hypercubes: List<HyperCube>,
        dataset: DataFrame,
        step: Int,
        adaptive: Any?
    ): List<HyperCube> = sequence {
        for (cube in hypercubes)
            when {
                cube.count(dataset) == 0 -> continue
                cube.std < threshold -> yield(cube)
                else -> yieldAll(splitCube(cube, dataset, step, adaptive))
            }
    }.toList()

    private fun splitCube(
        cube: HyperCube,
        dataset: DataFrame,
        step: Int,
        adaptive: Any?
    ): List<HyperCube> {
        val splits: SplitFeature
        if (adaptive != null)
            throw NotImplementedError()
        else
            splits = dataset.inputs().schema().fields().associate {
                it.name to splitFeature(cube, it.name, step)
            }
        var newCubes = listOf<TempCube>(emptyMap())
        for (split in splits)
            newCubes = cartesianProduct(newCubes, split)
        return newCubes.map { HyperCube(it.toMutableMap()) }
    }

    private fun cartesianProduct(
        cubes: List<TempCube>,
        split: SplitEntry
    ): List<TempCube> {
        val newCubes = mutableListOf<TempCube>()
        for (limits in split.value)
            for (cube in cubes)
                newCubes.add(cube.plus(mapOf(split.key to limits)))
        return newCubes
    }

    private fun splitFeature(
        cube: HyperCube,
        feature: String,
        step: Int
    ): List<Pair<Double, Double>> {
        val (min, max) = cube.dimensions[feature]!!
        val dimension = (max - min) / step
        return sequence {
            for (i in 1..step)
                yield(min + dimension * (i - 1) to min + dimension * i)
        }.toList()
    }

    private fun adaptiveSplits(): Any? {
        return null
    }

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