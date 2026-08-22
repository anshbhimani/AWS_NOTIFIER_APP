package com.ansh.awsnotifier

import com.ansh.awsnotifier.service.StructuredAlertParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun parseStructuredAlert_cloudWatchPayload_returnsAlarmNameAndReason() {
        val raw = """
            {
              "AlarmName": "HighCPU",
              "NewStateReason": "Threshold Crossed: 1 datapoint [95.0] was greater than the threshold (80.0)."
            }
        """.trimIndent()

        val (title, body) = StructuredAlertParser.parse(raw)

        assertEquals("HighCPU", title)
        assertEquals(
            "Threshold Crossed: 1 datapoint [95.0] was greater than the threshold (80.0).",
            body
        )
    }

    @Test
    fun parseStructuredAlert_budgetPayload_returnsBudgetNameAndSpendMessage() {
        val raw = """
            {
              "BudgetName": "ProdMonthlyBudget",
              "ActualAmount": "105.50",
              "BudgetLimit": "100.00",
              "Unit": "USD"
            }
        """.trimIndent()

        val (title, body) = StructuredAlertParser.parse(raw)

        assertEquals("ProdMonthlyBudget", title)
        assertEquals(
            "Spend of 105.50 USD has crossed your budget limit of 100.00 USD",
            body
        )
    }

    @Test
    fun parseStructuredAlert_nonJson_returnsRawBody() {
        val raw = "Simple plain-text alert body"

        val (title, body) = StructuredAlertParser.parse(raw)

        assertNull(title)
        assertEquals(raw, body)
    }

    @Test
    fun parseStructuredAlert_unknownJson_returnsSummaryBody() {
        val raw = """
            {
              "eventType": "SecurityHubFinding",
              "severity": "HIGH",
              "account": "123456789012"
            }
        """.trimIndent()

        val (title, body) = StructuredAlertParser.parse(raw)

        assertNull(title)
        assertNotNull(body)
        assertEquals(
            "eventType: SecurityHubFinding\nseverity: HIGH\naccount: 123456789012",
            body
        )
    }
}