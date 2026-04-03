package dev.autopsy.db.repository

import dev.autopsy.db.entity.Report
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface ReportRepository : CrudRepository<Report, UUID> {

    @Query("SELECT * FROM reports WHERE repo_id = :repoId ORDER BY created_at DESC")
    fun findByRepoId(repoId: UUID): List<Report>

    @Query("SELECT * FROM reports WHERE repo_id = :repoId ORDER BY created_at DESC LIMIT 1")
    fun findLatestByRepoId(repoId: UUID): Report?

    @Query("SELECT * FROM reports WHERE repo_id = :repoId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    fun findByRepoIdPaginated(repoId: UUID, limit: Int, offset: Int): List<Report>

    @Query("SELECT COUNT(*) FROM reports WHERE repo_id = :repoId")
    fun countByRepoId(repoId: UUID): Int
}
