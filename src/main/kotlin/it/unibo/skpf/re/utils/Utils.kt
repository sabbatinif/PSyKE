package it.unibo.skpf.re.utils

import it.unibo.skpf.re.Extractor
import java.io.*
import kotlin.math.pow

internal fun saveToFile(filename: String, item: Any) {
    val file = File("src").resolve("test").resolve("resources").resolve(filename)
    return ObjectOutputStream(FileOutputStream(file)).use {
        it.writeObject(item)
    }
}

fun loadFromFile(filename: String): Any {
    val file = Extractor::class.java.getResourceAsStream("/$filename")!!
    return ObjectInputStream(file).use {
        it.readObject()
    }
}

fun Double.round(digits: Int = 2): Double {
    val k = 10.0.pow(digits)
    return kotlin.math.round(this * k) / k
}

internal inline fun <reified C, R> C.getFieldValue(name: String): R {
    val valueField = C::class.java.getDeclaredField(name)
    valueField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return valueField.get(this) as R
}