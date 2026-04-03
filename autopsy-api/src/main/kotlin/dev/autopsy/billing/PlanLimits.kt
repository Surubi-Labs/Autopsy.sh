package dev.autopsy.billing

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class PlanLimits(
    @Value("\${autopsy.stripe.pro-price-id:}") private val proPriceId: String,
    @Value("\${autopsy.stripe.team-price-id:}") private val teamPriceId: String,
) {
    companion object {
        private const val FREE_REPO_LIMIT = 1
        private const val PRO_REPO_LIMIT = 10
        private const val TEAM_REPO_LIMIT = 9999

        fun repoLimitForPlan(plan: String): Int = when (plan.lowercase()) {
            "pro" -> PRO_REPO_LIMIT
            "team" -> TEAM_REPO_LIMIT
            else -> FREE_REPO_LIMIT
        }
    }

    fun planForPriceId(priceId: String): String? = when (priceId) {
        proPriceId -> "pro"
        teamPriceId -> "team"
        else -> null
    }
}
