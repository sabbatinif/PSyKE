package it.unibo.skpf.re

fun main() {
    classify("iris.data", 0.5)
    classifyWithoutDiscretise("iris.data", 0.5)
//    classify("car.data", 0.2)
    regression("arti.csv", 0.2)


}
