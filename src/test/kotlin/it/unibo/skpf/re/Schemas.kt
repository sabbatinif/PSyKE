package it.unibo.skpf.re

import it.unibo.skpf.re.schema.Feature
import it.unibo.skpf.re.schema.Schema
import it.unibo.skpf.re.schema.Value.Interval.Between
import it.unibo.skpf.re.schema.Value.Interval.GreaterThan
import it.unibo.skpf.re.schema.Value.Interval.LessThan

object Schemas {
    val iris = Schema.Ordered(
        Feature(
            "SepalLength",
            "SepalLength_0" to LessThan(5.39),
            "SepalLength_1" to Between(5.39, 6.26),
            "SepalLength_2" to GreaterThan(6.26)
        ),
        Feature(
            "SepalWidth",
            "SepalWidth_0" to LessThan(2.87),
            "SepalWidth_1" to Between(2.87, 3.2),
            "SepalWidth_2" to GreaterThan(3.2)
        ),
        Feature(
            "PetalLength",
            "PetalLength_0" to LessThan(2.28),
            "PetalLength_1" to Between(2.28, 4.87),
            "PetalLength_2" to GreaterThan(4.87)
        ),
        Feature(
            "PetalWidth",
            "PetalWidth_0" to LessThan(0.65),
            "PetalWidth_1" to Between(0.65, 1.64),
            "PetalWidth_2" to GreaterThan(1.64)
        )
    )
}
