package it.unibo.skpf.re

import org.apache.commons.csv.CSVFormat
import smile.base.cart.SplitRule
import smile.base.mlp.Layer
import smile.base.mlp.OutputFunction
import smile.classification.cart
import smile.classification.knn
import smile.classification.mlp
import smile.data.*
import smile.data.formula.Formula
import smile.io.Read
import java.time.Instant

fun main() {
    // reading the data set as CSV with header
    // i.e. first row with column names
    val dataset = Read.csv("datasets/iris.data", CSVFormat.DEFAULT.withHeader())

    // discretisation schema
    val featureSets = dataset.splitFeatures()
    // data set discretisation according to the schema
    // + train/test splitting
    val (train, test) = dataset.toBoolean(featureSets).randomSplit(0.5)

    // conversion from Smile DataFrame to Array
    val x = train.inputsArray()
    val y = train.classesArray()

    // creation and training of the models
    val predictor = knn(x, y, 9)
    val ann = mlp(x, y,
        arrayOf(
            Layer.sigmoid(train.ncols()),
            Layer.sigmoid(15),
            Layer.sigmoid(5),
            Layer.mle(train.nCategories(), OutputFunction.SOFTMAX)
        ),
        epochs = 25
    )
    val decTree = cart(
        Formula.lhs("Class"),
        train.inputs().merge(train.classes()),
        SplitRule.GINI,
        20, 0, 5
    )

    // print the prediction performances of the predictors w.r.t. the data
    // printMatrix = true for the confusion matrix
    println("*** KNN ***")
    testClassifier(test, predictor, printAccuracy = true, printMatrix = false).also { println() }
    println("*** MLP ***")
    testClassifier(test, ann, printAccuracy = true, printMatrix = false).also { println() }
    println("*** TREE ***")
    testClassifier(test, decTree, printAccuracy = true, printMatrix = false).also { println() }

    // train the extractor with the predictors
    val realKNN = Extractor.ruleExtractionAsLearning(predictor, featureSets)
    val realMLP = Extractor.ruleExtractionAsLearning(ann, featureSets)
    val duepanKNN = Extractor.duepan(predictor, featureSets)
    val duepanMLP = Extractor.duepan(ann, featureSets)
    // cart requires a DecisionTree
    val cart = Extractor.cart(decTree, featureSets)

    // test the performances of the extractors
    testClassificationExtractor("REAL with KNN", train, test, realKNN, predictor, true, true, true)
    testClassificationExtractor("DUEPAN with MLP", train, test, duepanMLP, ann, true, true, true)
    testClassificationExtractor("CART", train, test, cart, true, true, true)

    // test CART without discretising the data set
    classifyWithoutDiscretise("iris.data", 0.5)

//    classify("iris.data", 0.5)
//    classify("car.data", 0.2)
//    regression("arti.csv", 0.2)
}
