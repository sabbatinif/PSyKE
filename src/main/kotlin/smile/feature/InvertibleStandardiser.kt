package smile.feature

import smile.data.DataFrame
import smile.data.Tuple
import smile.data.type.StructType
import smile.math.MathEx

internal class InvertibleStandardiser(schema: StructType, mu: DoubleArray, std: DoubleArray) :
    Standardizer(schema, mu, std), InvertibleFeatureTransform {

    override val schema: StructType?
        get() = super.schema

    val mean: DoubleArray
        get() = super.mu

    val stDev: DoubleArray
        get() = super.std

    override fun invert(x: Double, i: Int): Double {
        return this.stDev[i] * x + this.mean[i]
    }

    companion object {
        @JvmStatic
        fun fit(data: DataFrame): InvertibleStandardiser {
            require(!data.isEmpty) { "Empty data frame" }

            val schema = data.schema()
            val mu = DoubleArray(schema.length())
            val std = DoubleArray(schema.length())

            val n = data.nrows()
            for (i in mu.indices) {
                if (schema.field(i).isNumeric) {
                    val sum = data.stream()
                        .mapToDouble { t: Tuple -> t.getDouble(i) }
                        .sum()
                    val squaredSum = data.stream()
                        .mapToDouble { t: Tuple -> t.getDouble(i) }
                        .map { x: Double -> x * x }.sum()
                    mu[i] = sum / n
                    std[i] = Math.sqrt(squaredSum / n - mu[i] * mu[i])
                    if (MathEx.isZero(std[i])) {
                        std[i] = 1.0
                    }
                }
            }

            return InvertibleStandardiser(schema, mu, std)
        }

        @JvmStatic
        fun fit(data: Array<DoubleArray>): InvertibleStandardiser = fit(DataFrame.of(data))
    }
}
