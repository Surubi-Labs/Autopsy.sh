package dev.autopsy.db.repository

import dev.autopsy.db.entity.Metric
import java.util.UUID

interface MetricRepository {
    fun findByRunId(runId: UUID): List<Metric>
    fun findByRepoId(repoId: UUID): List<Metric>
    fun findByRepoIdAndType(repoId: UUID, metricType: String): List<Metric>
    fun save(runId: UUID, repoId: UUID, modulePath: String?, metricType: String, value: Double, details: String?): Metric
    fun findByRepoIdAndModulePath(repoId: UUID, modulePath: String): List<Metric>
    fun findLatestByRepoIdGrouped(repoId: UUID): List<Metric>
}
