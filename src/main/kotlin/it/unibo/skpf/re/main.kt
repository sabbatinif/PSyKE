package it.unibo.skpf.re

import smile.classification.knn
import smile.io.Read
import smile.validation.metric.Accuracy

fun main() {
    val name = "iris"

    val dataset = Read.csv("datasets/$name.data")
    val featureSets = dataset.splitFeatures()
    val boolDataset = dataset.toBoolean(featureSets)
    val (train, test) = boolDataset.randomSplit(0.2)
    val x = train.inputsArray()
    val y = train.classesArray()
    val knn = knn(x, y, 9)
    println(
        "Classifier it.unibo.skpf.accuracy: " +
                Accuracy.of(test.classesArray(), knn.predict(test.inputsArray()))
    )
    val real = Extractor.ruleExtractionAsLearning(knn, featureSets)
    val realTheory = real.extract(train)
    println(
        "it.unibo.skpf.REAL it.unibo.skpf.fidelity: " +
                Accuracy.of(knn.predict(test.inputsArray()), real.predict(test))
    )
    val duepan = Extractor.duepan(knn, featureSets)
    val duepanTheory = duepan.extract(train)
    println(
        "it.unibo.skpf.Duepan it.unibo.skpf.fidelity: " +
                Accuracy.of(knn.predict(test.inputsArray()), duepan.predict(test))
    )

    realTheory.clauses.forEach { println(it.toString()) }
    println("****")
    duepanTheory.clauses.forEach { println(it.toString()) }

}
