package dev.autopsy.api

import dev.autopsy.api.dto.*
import dev.autopsy.auth.AuthenticatedOrg
import dev.autopsy.db.repository.MetricRepository
import dev.autopsy.db.repository.OrganizationRepository
import dev.autopsy.db.repository.RepoRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class DashboardController(
    private val repoRepo: RepoRepository,
    private val orgRepo: OrganizationRepository,
    private val metricRepo: MetricRepository,
) {

    @GetMapping("/dashboard")
    fun getDashboard(@AuthenticationPrincipal auth: AuthenticatedOrg): DashboardResponse {
        val org = orgRepo.findByIdOrNull(auth.orgId)
        val repos = repoRepo.findByOrgId(auth.orgId)

        val repoSummaries = repos.map { repo ->
            val repoId = requireNotNull(repo.id) { "RepoEntity ID must not be null" }
            val metrics = metricRepo.findLatestByRepoIdGrouped(repoId)
            val healthScore = metrics.find { it.metricType == "health_score" }?.value
            RepoHealthSummary(
                repoId = repoId,
                name = repo.name,
                healthScore = healthScore,
                lastAnalyzedAt = repo.lastAnalyzedAt,
            )
        }

        return DashboardResponse(
            orgName = org?.name ?: auth.orgName,
            plan = org?.plan ?: "free",
            repoCount = repos.size,
            repos = repoSummaries,
        )
    }

    @GetMapping("/dashboard/risks")
    fun getRisks(@AuthenticationPrincipal auth: AuthenticatedOrg): List<RiskItem> {
        // TODO: N+1 query pattern — replace with a single bulk metrics query per org
        val repos = repoRepo.findByOrgId(auth.orgId).take(50)
        val risks = mutableListOf<RiskItem>()

        for (repo in repos) {
            val repoId = requireNotNull(repo.id) { "RepoEntity ID must not be null" }
            val metrics = metricRepo.findLatestByRepoIdGrouped(repoId)
            for (metric in metrics) {
                if (metric.metricType == "bus_factor" && metric.value <= 1.0) {
                    risks.add(
                        RiskItem(
                            repoId = repoId,
                            repoName = repo.name,
                            riskType = "bus_factor",
                            severity = "critical",
                            description = "Module ${metric.modulePath ?: "unknown"} has bus factor of ${metric.value.toInt()}",
                        ),
                    )
                }
                if (metric.metricType == "dep_staleness" && metric.value < 50.0) {
                    risks.add(
                        RiskItem(
                            repoId = repoId,
                            repoName = repo.name,
                            riskType = "dependency",
                            severity = "warning",
                            description = "Dependency health score: ${metric.value.toInt()}/100",
                        ),
                    )
                }
            }
        }

        return risks.sortedBy { it.severity }.take(20)
    }

}
