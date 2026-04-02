package dev.autopsy.db.repository

import dev.autopsy.db.entity.Organization
import java.util.UUID

interface OrganizationRepository {
    fun findById(id: UUID): Organization?
    fun save(name: String, plan: String = "free", repoLimit: Int = 3): Organization
}
