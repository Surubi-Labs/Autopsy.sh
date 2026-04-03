package dev.autopsy.db.repository

import dev.autopsy.db.entity.Metric
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface MetricRepository : CrudRepository<Metric, UUID> {

    @Query("SELECT * FROM metrics WHERE run_id = :runId ORDER BY computed_at")
    fun findByRunId(runId: UUID): List<Metric>

    @Query("SELECT * FROM metrics WHERE repo_id = :repoId ORDER BY computed_at DESC")
    fun findByRepoId(repoId: UUID): List<Metric>

    @Query("SELECT * FROM metrics WHERE repo_id = :repoId AND metric_type = :metricType ORDER BY computed_at DESC")
    fun findByRepoIdAndType(repoId: UUID, metricType: String): List<Metric>

    @Query("SELECT * FROM metrics WHERE repo_id = :repoId AND module_path = :modulePath ORDER BY computed_at DESC")
    fun findByRepoIdAndModulePath(repoId: UUID, modulePath: String): List<Metric>

    @Query("""
        SELECT DISTINCT ON (metric_type) *
        FROM metrics WHERE repo_id = :repoId
        ORDER BY metric_type, computed_at DESC
    """)
    fun findLatestByRepoIdGrouped(repoId: UUID): List<Metric>
}
