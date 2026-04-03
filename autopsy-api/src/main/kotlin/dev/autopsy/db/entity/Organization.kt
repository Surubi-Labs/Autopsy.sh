package dev.autopsy.db.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("organizations")
data class Organization(
    @Id val id: UUID? = null,
    val name: String,
    val plan: String = "free",
    val stripeCustomer: String? = null,
    val repoLimit: Int = 1,
    val emailAddress: String? = null,
    val slackWebhookUrl: String? = null,
    val stripeSubscriptionId: String? = null,
    val stripePriceId: String? = null,
    val planUpdatedAt: Instant? = null,
    val createdAt: Instant? = null,
)
