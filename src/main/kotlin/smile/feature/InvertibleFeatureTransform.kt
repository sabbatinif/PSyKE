package smile.feature

import smile.data.AbstractTuple
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.type.StructType
import smile.data.vector.BaseVector
import smile.data.vector.DoubleVector

interface InvertibleFeatureTransform : FeatureTransform {

    val schema: StructType?

    fun invert (x: Double, i: Int): Double

    fun invert(x: DoubleArray?): DoubleArray? {
        val y = DoubleArray(x!!.size)
        for (i in y.indices) {
            y[i] = this.invert(x[i], i)
        }
        return y
    }

    fun invert(data: Array<DoubleArray?>?): Array<DoubleArray?>? {
        val n = data!!.size
        val y = arrayOfNulls<DoubleArray>(n)
        for (i in 0 until n) {
            y[i] = invert(data[i])
        }
        return y
    }

    fun invert(x: Tuple?): Tuple? {
        require(schema == x!!.schema()) { "Invalid schema" }

        return object : AbstractTuple() {
            override fun get(i: Int): Any {
                return if (schema!!.field(i).isNumeric) {
                    invert(x.getDouble(i), i)
                } else {
                    x[i]
                }
            }

            override fun schema(): StructType {
                return schema!!
            }
        }
    }

    fun invert(data: DataFrame?): DataFrame? {
        require(schema == data!!.schema()) { "Invalid schema" }

        val vectors: Array<BaseVector<*, *, *>?> = arrayOfNulls(schema!!.length())
        for (i in 0 until schema!!.length()) {
            val field = schema!!.field(i)
            if (field.isNumeric) {
                val stream = data.stream().mapToDouble { invert(it.getDouble(i), i) }
                vectors[i] = DoubleVector.of(field, stream)
            } else {
                vectors[i] = data.column(i)
            }
        }
        return DataFrame.of(*vectors)
    }

    companion object {
        fun scaler(data: DataFrame): InvertibleFeatureTransform = InvertibleScaler.fit(data)

        fun scaler(data: Array<DoubleArray>): InvertibleFeatureTransform = InvertibleScaler.fit(data)

        fun standardiser(data: DataFrame): InvertibleFeatureTransform = InvertibleStandardiser.fit(data)

        fun standardiser(data: Array<DoubleArray>): InvertibleFeatureTransform = InvertibleStandardiser.fit(data)
    }
}