package it.unibo.skpf.re

import smile.classification.*
import smile.data.*
import smile.io.Read

fun main() {
    val name = "iris"

    val dataset = Read.csv("datasets/$name.data")
    val featureSets = dataset.splitFeatures()
    val boolDataset = dataset.toBoolean(featureSets)
    val (train, test) = boolDataset.randomSplit(0.5)
    val x = train.inputsArray()
    val y = train.classesArray()
    val knn = knn(x, y, 9)
    //saveToFile("irisKNN9.txt", knn)
    //val knn = loadFromFile("ann.txt") as KNN<DoubleArray>
    println("Classifier accuracy: " + accuracy(test, knn))
    println(confusionMatrix(test, knn))
    val real = Extractor.ruleExtractionAsLearning(knn, featureSets)
    //saveToFile("realIrisKNN9.txt", real)
    val realTheory = real.extract(train)
    println("REAL fidelity: " + fidelity(test, knn, real))
    println(confusionMatrix(test, knn, real))
    val duepan = Extractor.duepan(knn, featureSets)
    //saveToFile("duepanIrisKNN9.txt", duepan)
    val duepanTheory = duepan.extract(train)
    println("Duepan fidelity: " + fidelity(test, knn, duepan))
    println(confusionMatrix(test, knn, duepan))

    realTheory.clauses.forEach { println(it.toString()) }
    println("****")
    duepanTheory.clauses.forEach { println(it.toString()) }

}
