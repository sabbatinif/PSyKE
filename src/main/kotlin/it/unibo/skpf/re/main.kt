package it.unibo.skpf.re

import smile.classification.*
import smile.data.*
import smile.io.Read

fun main() {

    val tests = listOf(Pair("iris", 0.5), Pair("car", 0.2))

    for ((name, testSplit) in tests) {
        println("*** $name ***")
        val dataset = Read.csv("datasets/$name.data")
        val featureSets = dataset.splitFeatures()
        val (train, test) = dataset.toBoolean(featureSets).randomSplit(testSplit)
        val x = train.inputsArray()
        val y = train.classesArray()
        val knn = knn(x, y, 9)
//        saveToFile("irisKNN9.txt", knn)
//        val knn = loadFromFile("ann.txt") as KNN<DoubleArray>
        testClassifier(test, knn)
        val real = Extractor.ruleExtractionAsLearning(knn, featureSets)
        testClassificationExtractor("REAL", train, test, real, knn)
//        saveToFile("realIrisKNN9.txt", real)
        val duepan = Extractor.duepan(knn, featureSets)
//        saveToFile("duepanIrisKNN9.txt", duepan)
        testClassificationExtractor("Duepan", train, test, duepan, knn)
        println()
    }
}
