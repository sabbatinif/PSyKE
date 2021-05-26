package it.unibo.skpf.re

import it.unibo.skpf.re.Rule
import it.unibo.skpf.re.splitFeatures
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
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
        assertEquals(reducedRule.truePred, rule.reduce(featureSets).truePred)
        assertEquals(reducedRule.falsePred, rule.reduce(featureSets).falsePred)
        assertEquals(reducedRule.truePred, reducedRule.reduce(featureSets).truePred)
        assertEquals(reducedRule.falsePred, reducedRule.reduce(featureSets).falsePred)
    }
}