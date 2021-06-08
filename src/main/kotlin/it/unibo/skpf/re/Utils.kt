package it.unibo.skpf.re

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.math.pow

fun saveToFile(filename: String, item: Any) {
    val file = FileOutputStream("src\\test\\resources\\$filename")
    val outStream = ObjectOutputStream(file)
    outStream.writeObject(item)
    outStream.close()
    file.close()
}

fun loadFromFile(filename: String): Any {
    val file = FileInputStream("src\\test\\resources\\$filename")
    val inStream = ObjectInputStream(file)
    val item = inStream.readObject()
    inStream.close()
    file.close()
    return item
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