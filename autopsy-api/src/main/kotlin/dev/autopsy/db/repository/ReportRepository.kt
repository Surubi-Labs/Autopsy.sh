package dev.autopsy.db.repository

import dev.autopsy.db.entity.Report
import java.util.UUID

interface ReportRepository {
    fun findById(id: UUID): Report?
    fun findByRepoId(repoId: UUID): List<Report>
    fun findLatestByRepoId(repoId: UUID): Report?
    fun save(runId: UUID, repoId: UUID, markdown: String, summary: String?, riskScore: Double?): Report
    fun findByRepoIdPaginated(repoId: UUID, limit: Int, offset: Int): List<Report>
    fun countByRepoId(repoId: UUID): Int
}
