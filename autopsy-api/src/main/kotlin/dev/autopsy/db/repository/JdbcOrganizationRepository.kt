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
        val params = MapSqlParameterSource("id", id)
        return jdbc.query(sql, params) { rs, _ -> Organization.fromRow(rs) }.firstOrNull()
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
}
