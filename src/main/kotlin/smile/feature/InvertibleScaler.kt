package smile.feature

import smile.data.AbstractTuple
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.type.StructType
import smile.data.vector.BaseVector
import smile.data.vector.DoubleVector

internal class InvertibleScaler(schema: StructType, lo: DoubleArray, hi: DoubleArray) : Scaler(schema, lo, hi), InvertibleFeatureTransform {
    val lowerBounds: DoubleArray
        get() =  super.lo

    val upperBounds: DoubleArray
        get() = super.hi

    private fun invert (x: Double, i: Int): Double {
        return this.upperBounds[i] * x + this.lowerBounds[i]
    }

    override fun invert(x: DoubleArray?): DoubleArray? {
        val y = DoubleArray(x!!.size)
        for (i in y.indices) {
            y[i] = this.invert(x[i], i)
        }
        return y
    }

    override fun invert(data: Array<DoubleArray?>): Array<DoubleArray?>? {
        val n = data.size
        val y = arrayOfNulls<DoubleArray>(n)
        for (i in 0 until n) {
            y[i] = invert(data[i])
        }
        return y
    }

    override fun invert(x: Tuple?): Tuple? {
        require(schema == x!!.schema()) { "Invalid schema" }

        return object : AbstractTuple() {
            override fun get(i: Int): Any {
                return if (schema.field(i).isNumeric) {
                    invert(x.getDouble(i), i)
                } else {
                    x[i]
                }
            }

            override fun schema(): StructType {
                return schema
            }
        }
    }

    override fun invert(data: DataFrame?): DataFrame? {
        require(schema == data!!.schema()) { "Invalid schema" }

        val vectors: Array<BaseVector<*, *, *>?> = arrayOfNulls(schema.length())
        for (i in this.lowerBounds.indices) {
            val field = schema.field(i)
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
        @JvmStatic
        fun fit(data: DataFrame): InvertibleScaler {
            require(!data.isEmpty) { "Empty data frame" }

            val schema = data.schema()
            val lo = DoubleArray(schema.length())
            val hi = DoubleArray(schema.length())

            for (i in lo.indices) {
                if (schema.field(i).isNumeric) {
                    lo[i] = data.doubleVector(i).stream().min().asDouble
                    hi[i] = data.doubleVector(i).stream().max().asDouble
                }
            }

            return InvertibleScaler(schema, lo, hi)
        }

        @JvmStatic
        fun fit(data: Array<DoubleArray>): InvertibleScaler = fit(DataFrame.of(data))
    }
}