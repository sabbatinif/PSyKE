import it.unibo.skpf.re.utils.loadFromFile

fun main() {
    val files = listOf<String>(
        "artiRBF95.txt",
        "artiRBF95.txt",
        "artiTest50.txt",
        "artiTrain50.txt",
        "irisBoolFeatSet.txt",
        "irisKNN9.txt",
        "irisTest50.txt",
        "irisTrain50.txt",
    )

    for (file in files) {
        val x: Any = loadFromFile(file)
        println(x)
    }
}
