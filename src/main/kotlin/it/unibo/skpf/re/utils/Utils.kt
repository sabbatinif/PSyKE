package it.unibo.skpf.re.utils

import it.unibo.skpf.re.Extractor
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.math.pow

internal fun saveToFile(filename: String, item: Any) {
    val file = File("src").resolve("test").resolve("resources").resolve(filename)
    return ObjectOutputStream(FileOutputStream(file)).use {
        it.writeObject(item)
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> loadFromFile(filename: String): T {
    val file = Extractor::class.java.getResourceAsStream("/$filename")!!
    return ObjectInputStream(file).use {
        it.readObject()
    } as T
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
