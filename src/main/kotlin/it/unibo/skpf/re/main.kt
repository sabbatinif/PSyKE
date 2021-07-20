package it.unibo.skpf.re

import it.unibo.skpf.re.utils.classify
import org.apache.commons.csv.CSVFormat
import smile.data.createRanges
import smile.data.description
import smile.data.splitFeatures
import smile.io.Read

/**
 * Project main function.
 */
fun main() {
//    classify("iris.data", 0.5)

    val dataset = Read.csv("datasets/iris.data", CSVFormat.DEFAULT.withHeader())
    println(dataset.createRanges("PetalLength"))
    println(dataset.description)
    println(dataset.splitFeatures())


//    classifyWithoutDiscretise("iris.data", 0.5)
//    classify("car.data", 0.5 / 2)
//    regression("arti.csv", 0.5)
}
