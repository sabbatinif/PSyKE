package it.unibo.skpf.re

import smile.classification.accuracy
import smile.classification.confusionMatrix
import smile.classification.fidelity
import smile.classification.knn
import smile.data.*
import smile.io.Read
import smile.validation.metric.Accuracy

fun main() {
    val name = "car"

    val dataset = Read.csv("datasets/$name.data")
    val featureSets = dataset.splitFeatures()
    val boolDataset = dataset.toBoolean(featureSets)
    val (train, test) = boolDataset.randomSplit(0.5)
    val x = train.inputsArray()
    val y = train.classesArray()
    val knn = knn(x, y, 9)
    println("Classifier accuracy: " + accuracy(test, knn))
    println(confusionMatrix(test, knn))
    val real = Extractor.ruleExtractionAsLearning(knn, featureSets)
    val realTheory = real.extract(train)
    println("REAL fidelity: " + fidelity(test, knn, real))
    println(confusionMatrix(test, knn, real))
    val duepan = Extractor.duepan(knn, featureSets)
    val duepanTheory = duepan.extract(train)
    println("Duepan fidelity: " + fidelity(test, knn, duepan))
    println(confusionMatrix(test, knn, duepan))

    realTheory.clauses.forEach { println(it.toString()) }
    println("****")
    duepanTheory.clauses.forEach { println(it.toString()) }

}
