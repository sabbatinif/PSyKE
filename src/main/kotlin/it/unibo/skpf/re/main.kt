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
    classify("iris.data", 0.5)
    classifyWithoutDiscretise("iris.data", 0.5)
//    classify("car.data", 0.2)
//    regression("arti.csv", 0.2)

//        val cart2 = smile.regression.cart(
//            Formula.lhs("Class"),
//            train.inputs().merge(train.classes()),
//            20, 0, 5
//        )
}
