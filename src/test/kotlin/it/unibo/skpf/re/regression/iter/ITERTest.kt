package it.unibo.skpf.re.regression.iter

import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.utils.loadFromFile
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import smile.data.DataFrame
import smile.regression.RBFNetwork

internal class ITERTest {

    private val rbf = loadFromFile("artiRBF95.txt") as RBFNetwork<DoubleArray>
    private val iter = Extractor.iter(rbf, minUpdate = 1.0 / 20, threshold = 0.19)
    private val train = loadFromFile("artiTrain50.txt") as DataFrame
    private val theory = iter.extract(train)

    @Test
    fun extract() {
    }

    @Test
    fun predict() {
    }
}