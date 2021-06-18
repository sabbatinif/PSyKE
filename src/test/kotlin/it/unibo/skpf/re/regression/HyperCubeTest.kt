package it.unibo.skpf.re.regression

import it.unibo.skpf.re.regression.iter.Expansion
import it.unibo.skpf.re.regression.iter.MinUpdate
import it.unibo.skpf.re.utils.loadFromFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.description
import smile.data.inputs
import smile.regression.RBFNetwork
import java.util.stream.Stream
import kotlin.math.ceil
import kotlin.math.floor

internal class HyperCubeTest {

    fun createCube(x: Pair<Double, Double>, y: Pair<Double, Double>, output: Double) =
        HyperCube(
            mapOf(
                "X" to x,
                "Y" to y
            ).toMutableMap(),
            output
        )

    private val dataset = loadFromFile("artiTrain50.txt") as DataFrame
    private val dimensions = mapOf(
        "X" to Pair(0.2, 0.6),
        "Y" to Pair(0.7, 0.9)
    )
    private val output = 0.5
    private val cube = HyperCube(dimensions.toMutableMap(), output)
    private val filteredDataset = DataFrame.of(
        dataset.stream().filter { tuple ->
            (0.2 <= tuple.getDouble("X")) &&
                (tuple.getDouble("X") < 0.6) &&
                (0.7 <= tuple.getDouble("Y")) &&
                (tuple.getDouble("Y") < 0.9)
        }
    )

    private val hyperCubes = listOf(
        Triple(Pair(6.4, 7.9), Pair(5.7, 8.9), 5.3),
        Triple(Pair(0.7, 0.8), Pair(0.75, 0.85), 6.1),
        Triple(Pair(6.6, 7.0), Pair(9.1, 10.5), 7.5)
    ).map { createCube(it.first, it.second, it.third) }

    @Test
    fun testGetDimensions() {
        assertEquals(dimensions, cube.dimensions)
    }

    @Test
    fun testGetMean() {
        assertEquals(output, cube.mean)
    }

    @Test
    fun testGet() {
        assertEquals(Pair(0.2, 0.6), cube.get("X"))
        assertEquals(Pair(0.7, 0.9), cube.get("Y"))
        assertThrows(FeatureNotFoundException::class.java) { cube.get("Z") }
    }

    @Test
    fun testGetFirst() {
        assertEquals(0.2, cube.getFirst("X"))
        assertEquals(0.7, cube.getFirst("Y"))
        assertThrows(FeatureNotFoundException::class.java) { cube.getFirst("Z") }
    }

    @Test
    fun testGetSecond() {
        assertEquals(0.6, cube.getSecond("X"))
        assertEquals(0.9, cube.getSecond("Y"))
        assertThrows(FeatureNotFoundException::class.java) { cube.getSecond("Z") }
    }

    @Test
    fun testCopy() {
        val copy = cube.copy()
        assertEquals(cube.dimensions, copy.dimensions)
        assertEquals(cube.mean, copy.mean)
    }

    @ParameterizedTest
    @ArgumentsSource(ExpansionProvider::class)
    fun testExpand(
        cube: HyperCube,
        expansion: Expansion,
        expected: Pair<Double, Double>
    ) {
        cube.expand(expansion, hyperCubes)
        assertEquals(expected, cube.get(expansion.feature))
    }

    @Test
    fun testExpandAll() {
        val updates = listOf(
            MinUpdate("X", 0.2),
            MinUpdate("Y", 0.15)
        )
        val surrounding =
            createCube(Pair(0.0, 0.8), Pair(0.1, 0.6), 0.0)
        val cube =
            createCube(Pair(0.1, 0.2), Pair(0.4, 0.4), 0.4)
        cube.expandAll(updates, surrounding)
        assertEquals(Pair(0.0, 0.4), cube.dimensions["X"])
        assertEquals(Pair(0.25, 0.55), cube.dimensions["Y"])
    }

    @Test
    fun testOverlap() {
        assertNull(cube.overlap(hyperCubes))
        assertFalse(cube.overlap(hyperCubes[0]))
        assertFalse(cube.overlap(hyperCubes[1]))
        cube.updateDimension("X", 0.6, 1.0)
        assertNotNull(cube.overlap(hyperCubes))
        assertEquals(hyperCubes[1], cube.overlap(hyperCubes))
        assertFalse(cube.overlap(hyperCubes[0]))
        assertTrue(cube.overlap(hyperCubes[1]))
    }

    @Test
    fun testHasVolume() {
        assertTrue(cube.hasVolume())
        val noVolume = cube.copy()
        noVolume.updateDimension("X", 1.0, 1.0)
        assertFalse(noVolume.hasVolume())
    }

    @Test
    fun testEqual() {
        assertFalse(cube.equal(hyperCubes))
        assertTrue(hyperCubes[0].equal(hyperCubes))
    }

    @ParameterizedTest
    @ArgumentsSource(TupleProvider::class)
    fun testContains(tuple: Tuple, expected: Boolean) {
        assertEquals(expected, cube.contains(tuple))
    }

    @Test
    fun testCount() {
        assertEquals(dataset.nrows(), HyperCube.createSurroundingCube(dataset).count(dataset))
        assertEquals(filteredDataset.nrows(), cube.count(dataset))
    }

    @Test
    fun testCreateTuple() {
        val tuple = cube.createTuple(dataset.inputs().schema())
        cube.dimensions.forEach { (name, values) ->
            assertTrue(values.first <= tuple.getDouble(name))
            assertTrue(tuple.getDouble(name) < values.second)
        }
    }

    @Test
    fun testUpdateMean() {
        @Suppress("UNCHECKED_CAST")
        val predictor = loadFromFile("artiRBF95.txt") as RBFNetwork<DoubleArray>
        cube.updateMean(dataset.inputs(), predictor)
    }

    @Test
    fun testUpdateDimension() {
        val newLower = 0.6
        val newUpper = 1.4
        val updated = mapOf(
            "X" to Pair(newLower, newUpper),
            "Y" to Pair(0.7, 0.9)
        )
        val newCube1 = cube.copy()
        newCube1.updateDimension("X", newLower, newUpper)
        assertEquals(updated, newCube1.dimensions)
        val newCube2 = cube.copy()
        newCube2.updateDimension("X", Pair(newLower, newUpper))
        assertEquals(updated, newCube2.dimensions)
    }

    @Test
    fun testCreateSurroundingCube() {
        val surrounding = HyperCube.createSurroundingCube(dataset)
        dataset.description.apply {
            this.entries.forEach { (key, description) ->
                assertEquals(Pair(floor(description.min), ceil(description.max)), surrounding.dimensions[key])
            }
        }
    }

    @Test
    fun testCubeFromPoint() {
        val lower = 0.5
        val upper = 0.8
        val mean = 0.6
        val tuple = Tuple.of(
            doubleArrayOf(lower, upper, mean),
            dataset.schema()
        )
        val cube = HyperCube.cubeFromPoint(tuple)
        assertEquals(
            mapOf(
                "X" to Pair(lower, lower),
                "Y" to Pair(upper, upper)
            ),
            cube.dimensions
        )
        assertEquals(mean, cube.mean)
    }

    @Test
    fun testCheckOverlap() {
        assertTrue(
            HyperCube.checkOverlap(
                hyperCubes.toMutableList(),
                hyperCubes.plus(hyperCubes[0].copy())
            )
        )
        assertFalse(
            HyperCube.checkOverlap(
                hyperCubes.toMutableList(),
                hyperCubes
            )
        )
        assertFalse(
            HyperCube.checkOverlap(
                hyperCubes.subList(0, 1).toMutableList(),
                hyperCubes.subList(1, 3)
            )
        )
    }

    object TupleProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            val dataset = loadFromFile("artiTrain50.txt") as DataFrame
            return Stream.of(
                *mapOf(
                    doubleArrayOf(0.5, 0.8) to true,
                    doubleArrayOf(0.1, 0.8) to false,
                    doubleArrayOf(0.5, 0.95) to false,
                    doubleArrayOf(0.1, 0.95) to false
                ).map { (tuple, value) ->
                    Arguments.of(
                        Tuple.of(tuple, dataset.inputs().schema()), value
                    )
                }.toTypedArray()
            )
        }
    }

    object ExpansionProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            val cube1 = HyperCubeTest().createCube(
                Pair(2.3, 6.4),
                Pair(8.9, 12.3),
                2.3
            )
            val fake1 = cube1.copy()
            fake1.updateDimension("X", 0.5, 2.3)
            val fake2 = cube1.copy()
            fake2.updateDimension("X", 6.4, 12.9)
            val cube2 = cube1.copy()
            cube2.updateDimension("X", 9.5, 12.3)
            val fake3 = cube2.copy()
            fake3.updateDimension("X", 5.0, 9.5)
            val fake4 = cube2.copy()
            fake4.updateDimension("X", 12.3, 15.2)

            return Stream.of(
                Arguments.of(
                    cube1.copy(), Expansion(fake1, "X", '-', 0.0), Pair(0.5, 6.4)
                ),
                Arguments.of(
                    cube1.copy(), Expansion(fake2, "X", '+', 0.0), Pair(2.3, 6.6)
                ),
                Arguments.of(
                    cube2.copy(), Expansion(fake3, "X", '-', 0.0), Pair(7.0, 12.3)
                ),
                Arguments.of(
                    cube2.copy(), Expansion(fake4, "X", '+', 0.0), Pair(9.5, 15.2)
                )
            )
        }
    }
}
