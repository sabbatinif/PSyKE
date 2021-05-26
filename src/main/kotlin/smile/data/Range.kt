package smile.data

data class Range(
    val mean: Double,
    val std: Double
) {
    var lower: Double = this.mean
    var upper: Double = this.mean
}