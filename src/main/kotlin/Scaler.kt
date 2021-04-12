interface Scaler {
    fun transform(data: Array<DoubleArray>): Array<DoubleArray>
    fun inverse(data: Array<DoubleArray>): Array<DoubleArray>
}