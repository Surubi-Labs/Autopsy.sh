package dev.autopsy.api

import dev.autopsy.api.dto.MetricResponse
import dev.autopsy.api.dto.MetricsSummaryResponse
import dev.autopsy.api.dto.toResponse
import dev.autopsy.auth.AuthenticatedOrg
import dev.autopsy.db.repository.MetricRepository
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
class MetricsController(
    private val metricRepo: MetricRepository,
    private val orgScoped: OrgScopedService,
) {

    @GetMapping("/repos/{id}/metrics")
    fun getMetricsSummary(
        @AuthenticationPrincipal auth: AuthenticatedOrg,
        @PathVariable id: UUID,
    ): MetricsSummaryResponse {
        orgScoped.getRepoOrThrow(id, auth.orgId)
        val metrics = metricRepo.findLatestByRepoIdGrouped(id)
        val healthScore = metrics.find { it.metricType == "health_score" }?.value
        val breakdown = metrics.associate { it.metricType to it.value }
        val computedAt = metrics.maxOfOrNull { it.computedAt }
        return MetricsSummaryResponse(id, healthScore, breakdown, computedAt)
    }

    @GetMapping("/repos/{id}/metrics/{type}")
    fun getMetricsByType(
        @AuthenticationPrincipal auth: AuthenticatedOrg,
        @PathVariable id: UUID,
        @PathVariable type: String,
    ): List<MetricResponse> {
        orgScoped.getRepoOrThrow(id, auth.orgId)
        return metricRepo.findByRepoIdAndType(id, type).map { it.toResponse() }
    }

    @GetMapping("/repos/{id}/modules/{*path}/metrics")
    fun getModuleMetrics(
        @AuthenticationPrincipal auth: AuthenticatedOrg,
        @PathVariable id: UUID,
        @PathVariable path: String,
    ): List<MetricResponse> {
        orgScoped.getRepoOrThrow(id, auth.orgId)
        val cleanPath = path.trimStart('/')
        return metricRepo.findByRepoIdAndModulePath(id, cleanPath).map { it.toResponse() }
    }
}
