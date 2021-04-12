import org.jetbrains.bio.viktor._I
import org.jetbrains.bio.viktor.toF64Array

class MinMaxScaler : Scaler {
    private var min = DoubleArray(1) {0.0}
    private var max = DoubleArray(1) {1.0}
    val scaled: Array<DoubleArray> = arrayOf()

    override fun transform(data: Array<DoubleArray>): Array<DoubleArray> {
        val scaled: ArrayList<DoubleArray> = arrayListOf()
        min = DoubleArray(data[0].size) {0.0}
        max = DoubleArray(data[0].size) {1.0}
        val d = data.toF64Array()
        for (i in data[0].indices) {
            val feat = d.V[_I, i]
            min[i] = feat.min()
            max[i] = feat.max()
            d.V[_I, i] = (feat - min[i]) / (max[i] - min[i])
        }
        for (i in data.indices)
            scaled.add(d.V[i].toDoubleArray())
        return scaled.toTypedArray()
    }

    override fun inverse(data: Array<DoubleArray>): Array<DoubleArray> {
        val inverse: ArrayList<DoubleArray> = arrayListOf()
        val d = data.toF64Array()
        for (i in data[0].indices) {
            d.V[_I, i] = d.V[_I, i] * (max[i] - min[i]) + min[i]
        }
        for (i in data.indices)
            inverse.add(d.V[i].toDoubleArray())
        return inverse.toTypedArray()
    }
}