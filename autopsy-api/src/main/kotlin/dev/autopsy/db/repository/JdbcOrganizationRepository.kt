package dev.autopsy.db.repository

import dev.autopsy.db.entity.Organization
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JdbcOrganizationRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) : OrganizationRepository {

    override fun findById(id: UUID): Organization? {
        val sql = "SELECT * FROM organizations WHERE id = :id"
        return jdbc.query(sql, MapSqlParameterSource("id", id)) { rs, _ ->
            Organization.fromRow(rs)
        }.firstOrNull()
    }

    override fun findByStripeCustomerId(customerId: String): Organization? {
        val sql = "SELECT * FROM organizations WHERE stripe_customer = :cid"
        return jdbc.query(sql, MapSqlParameterSource("cid", customerId)) { rs, _ ->
            Organization.fromRow(rs)
        }.firstOrNull()
    }

    override fun save(name: String, plan: String, repoLimit: Int): Organization {
        val sql = """
            INSERT INTO organizations (name, plan, repo_limit)
            VALUES (:name, :plan, :repoLimit)
            RETURNING *
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("name", name)
            .addValue("plan", plan)
            .addValue("repoLimit", repoLimit)
        return jdbc.queryForObject(sql, params) { rs, _ -> Organization.fromRow(rs) }!!
    }

    override fun updatePlan(
        orgId: UUID,
        plan: String,
        repoLimit: Int,
        subscriptionId: String?,
        priceId: String?,
    ) {
        val sql = """
            UPDATE organizations
            SET plan = :plan, repo_limit = :repoLimit,
                stripe_subscription_id = :subId, stripe_price_id = :priceId,
                plan_updated_at = now()
            WHERE id = :id
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("id", orgId)
            .addValue("plan", plan)
            .addValue("repoLimit", repoLimit)
            .addValue("subId", subscriptionId)
            .addValue("priceId", priceId)
        jdbc.update(sql, params)
    }

    override fun updateStripeCustomer(orgId: UUID, stripeCustomerId: String) {
        val sql = "UPDATE organizations SET stripe_customer = :cid WHERE id = :id"
        jdbc.update(sql, MapSqlParameterSource().addValue("id", orgId).addValue("cid", stripeCustomerId))
    }

    override fun updateDeliveryPreferences(orgId: UUID, emailAddress: String?, slackWebhookUrl: String?) {
        val sql = """
            UPDATE organizations
            SET email_address = :email, slack_webhook_url = :slack
            WHERE id = :id
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("id", orgId)
            .addValue("email", emailAddress)
            .addValue("slack", slackWebhookUrl)
        jdbc.update(sql, params)
    }
}
