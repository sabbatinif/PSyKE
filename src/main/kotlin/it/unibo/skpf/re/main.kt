package it.unibo.skpf.re

import smile.base.cart.SplitRule
import smile.classification.*
import smile.data.*
import smile.data.formula.Formula
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
//        saveToFile("irisTest50.txt", test)
//        saveToFile("irisTrain50.txt", train)
//        saveToFile("irisBoolFeatSet.txt", featureSets)
        testClassifier(test, knn)
        val real = Extractor.ruleExtractionAsLearning(knn, featureSets)
        testClassificationExtractor("REAL", train, test, real, knn, printRules = true)
        val duepan = Extractor.duepan(knn, featureSets)
        testClassificationExtractor("Duepan", train, test, duepan, knn, printRules = true)
break
        // fun cart(formula: Formula,
        //             data: DataFrame,
        //             splitRule: SplitRule = SplitRule.GINI,
        //             maxDepth: Int = 20,
        //             maxNodes: Int = 0,
        //             nodeSize: Int = 5): RegressionTree
        //

//        val (train2, test2) = dataset.randomSplit(testSplit)
        val cart = cart(//) smile.regression.cart(
            Formula.lhs("Class"),
//            train2.inputs().merge(train.classes()),
            train.inputs().merge(train.classes()),
            SplitRule.GINI,
            20, 0, 5
        )
        val cartEx = Extractor.cart(cart, featureSets)
        testClassificationExtractor("CART", train, test, cartEx, true, false, true)
//        testClassificationExtractor("CART", train2, test2, cartEx, true, false, true)
    }
}
