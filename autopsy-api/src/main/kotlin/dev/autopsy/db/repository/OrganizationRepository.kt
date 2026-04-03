package dev.autopsy.db.repository

import dev.autopsy.db.entity.Organization
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface OrganizationRepository : CrudRepository<Organization, UUID> {

    fun findByStripeCustomer(stripeCustomer: String): Organization?

    @Modifying
    @Query("""
        UPDATE organizations SET plan = :plan, repo_limit = :repoLimit,
        stripe_subscription_id = :subscriptionId, stripe_price_id = :priceId,
        plan_updated_at = now() WHERE id = :orgId
    """)
    fun updatePlan(orgId: UUID, plan: String, repoLimit: Int, subscriptionId: String?, priceId: String?)

    @Modifying
    @Query("UPDATE organizations SET stripe_customer = :stripeCustomerId WHERE id = :orgId")
    fun updateStripeCustomer(orgId: UUID, stripeCustomerId: String)

    @Modifying
    @Query("UPDATE organizations SET email_address = :emailAddress, slack_webhook_url = :slackWebhookUrl WHERE id = :orgId")
    fun updateDeliveryPreferences(orgId: UUID, emailAddress: String?, slackWebhookUrl: String?)
}
