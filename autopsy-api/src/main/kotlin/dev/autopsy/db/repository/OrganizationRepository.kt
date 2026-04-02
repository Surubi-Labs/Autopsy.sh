package dev.autopsy.db.repository

import dev.autopsy.db.entity.Organization
import java.util.UUID

interface OrganizationRepository {
    fun findById(id: UUID): Organization?
    fun findByStripeCustomerId(customerId: String): Organization?
    fun save(name: String, plan: String = "free", repoLimit: Int = 1): Organization
    fun updatePlan(orgId: UUID, plan: String, repoLimit: Int, subscriptionId: String?, priceId: String?): Unit
    fun updateStripeCustomer(orgId: UUID, stripeCustomerId: String): Unit
    fun updateDeliveryPreferences(orgId: UUID, emailAddress: String?, slackWebhookUrl: String?): Unit
}
