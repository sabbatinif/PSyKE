package it.unibo.skpf.re.classification.real

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import smile.data.splitFeatures
import smile.io.Read

class RuleTest {
    @Test
    fun testSubrule() {
        val predList1 = listOf("V1", "V2")
        val predList2 = listOf("V3", "V4")
        val rule1 = Rule(predList1, predList2)
        assertTrue(rule1.subRule(rule1))
        val rule2 = Rule(predList2, predList1)
        assertFalse(rule1.subRule(rule2))
        assertFalse(rule2.subRule(rule1))
        val rule3 = Rule(listOf("V1"), listOf("V3"))
        assertTrue(rule1.subRule(rule3))
        assertFalse(rule3.subRule(rule1))
        assertFalse(rule2.subRule(rule3))
        assertFalse(rule3.subRule(rule2))
        val rule4 = Rule(listOf("V1"), listOf("V5"))
        assertFalse(rule1.subRule(rule4))
        assertFalse(rule4.subRule(rule1))
        val rule5 = Rule(listOf("V1", "V6"), listOf("V3", "V4"))
        assertFalse(rule1.subRule(rule5))
        assertFalse(rule5.subRule(rule1))
        assertTrue(rule1.subRule(Rule(listOf(), listOf())))
    }

    @Test
    fun testReduce() {
        val dataset = Read.csv("datasets/iris.data")
        val featureSets = dataset.splitFeatures()
        val rule = Rule(
            listOf("V1_1", "V2_2", "V3_0"),
            listOf("V1_0", "V2_1", "V2_0", "V4_1", "V4_2")
        )
        val reducedRule = Rule(
            listOf("V1_1", "V2_2", "V3_0"),
            listOf("V4_1", "V4_2")
        )
        assertEquals(reducedRule.truePredicates, rule.reduce(featureSets).truePredicates)
        assertEquals(reducedRule.falsePredicates, rule.reduce(featureSets).falsePredicates)
        assertEquals(reducedRule.truePredicates, reducedRule.reduce(featureSets).truePredicates)
        assertEquals(reducedRule.falsePredicates, reducedRule.reduce(featureSets).falsePredicates)
    }

    @Test
    fun testAsMutable() {
        val rule = Rule(
            listOf("V1_1", "V2_2", "V3_0"),
            listOf("V4_1", "V4_2")
        )
        assertEquals(
            listOf(
                mutableListOf("V1_1", "V2_2", "V3_0"),
                mutableListOf("V4_1", "V4_2")
            ),
            rule.asMutable()
        )
    }

    @Test
    fun testAsList() {
        val rule = Rule(
            listOf("V1_1", "V2_2", "V3_0"),
            listOf("V4_1", "V4_2")
        )
        assertEquals(
            listOf(
                listOf("V1_1", "V2_2", "V3_0"),
                listOf("V4_1", "V4_2")
            ),
            rule.asList()
        )
    }
}
