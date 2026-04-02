package dev.autopsy.db.entity

import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

data class Organization(
    val id: UUID,
    val name: String,
    val plan: String,
    val stripeCustomer: String?,
    val repoLimit: Int,
    val createdAt: Instant,
) {
    companion object {
        fun fromRow(rs: ResultSet): Organization = Organization(
            id = rs.getObject("id", UUID::class.java),
            name = rs.getString("name"),
            plan = rs.getString("plan"),
            stripeCustomer = rs.getString("stripe_customer"),
            repoLimit = rs.getInt("repo_limit"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }
}
