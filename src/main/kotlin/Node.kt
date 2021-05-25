import smile.data.DataFrame

class Node(val samples: DataFrame,
           val nExamples: Int,
           var constraints: Set<Pair<String, Double>> = emptySet(),
           var children: MutableList<Node> = mutableListOf()
) {
    fun priority(): Double {
        return -(this.reach() * (1 - this.fidelity()))
    }

    fun fidelity(): Double {
        return 1.0 * this.correct() / this.samples.nrows()
    }

    fun reach(): Double {
        return 1.0 * this.samples.nrows() / this.nExamples
    }

    fun correct(): Int {
        return this.samples.outputClasses().count {
            it == this.dominant()
        }
    }

    fun dominant(): Any {
        return this.samples.outputClasses()
            .groupBy { it }
            .mapValues { it.value.size }
            .maxBy { it.value }?.key ?: ""
    }

    fun nClasses(): Int {
        return this.samples.nCategories()
    }

    override fun toString(): String {
        var name = ""
        for (c in this.constraints)
            name += (if (c.second > 0) "" else "!") + c.first + ", "
        return name.dropLast(2) + " = " + this.dominant()
    }
}
