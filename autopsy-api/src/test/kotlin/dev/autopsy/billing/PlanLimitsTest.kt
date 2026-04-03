package dev.autopsy.billing

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlanLimitsTest {

    private val planLimits = PlanLimits("price_pro_123", "price_team_456")

    @Test
    fun `free plan has 1 repo limit`() {
        assertEquals(1, PlanLimits.repoLimitForPlan("free"))
    }

    @Test
    fun `pro plan has 10 repo limit`() {
        assertEquals(10, PlanLimits.repoLimitForPlan("pro"))
    }

    @Test
    fun `team plan has 9999 repo limit`() {
        assertEquals(9999, PlanLimits.repoLimitForPlan("team"))
    }

    @Test
    fun `unknown plan defaults to free limit`() {
        assertEquals(1, PlanLimits.repoLimitForPlan("enterprise"))
    }

    @Test
    fun `case insensitive plan lookup`() {
        assertEquals(10, PlanLimits.repoLimitForPlan("PRO"))
        assertEquals(9999, PlanLimits.repoLimitForPlan("Team"))
    }

    @Test
    fun `planForPriceId maps pro price`() {
        assertEquals("pro", planLimits.planForPriceId("price_pro_123"))
    }

    @Test
    fun `planForPriceId maps team price`() {
        assertEquals("team", planLimits.planForPriceId("price_team_456"))
    }

    @Test
    fun `planForPriceId returns null for unknown price`() {
        assertNull(planLimits.planForPriceId("price_unknown"))
    }
}
