package dev.autopsy.api.dto

import java.time.Instant
import java.util.UUID

// Pagination
data class PageResponse<T>(
    val data: List<T>,
    val total: Int,
    val limit: Int,
    val offset: Int,
)

// Auth
data class GitHubAuthRequest(val code: String)
data class AuthResponse(val token: String, val orgId: UUID, val orgName: String)

// Repos
data class ConnectRepoRequest(
    val name: String,
    val gitUrl: String,
    val defaultBranch: String = "main",
)

data class RepoResponse(
    val id: UUID,
    val name: String,
    val gitUrl: String,
    val defaultBranch: String,
    val schedule: String,
    val active: Boolean,
    val lastAnalyzedSha: String?,
    val lastAnalyzedAt: Instant?,
    val createdAt: Instant,
)

// Runs
data class RunResponse(
    val id: UUID,
    val repoId: UUID,
    val status: String,
    val currentStage: String?,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val error: String?,
    val commitRange: String?,
    val createdAt: Instant,
)

data class TriggerAnalysisResponse(val runId: UUID, val status: String = "queued")

// Metrics
data class MetricsSummaryResponse(
    val repoId: UUID,
    val healthScore: Double?,
    val breakdown: Map<String, Double>,
    val computedAt: Instant?,
)

data class MetricResponse(
    val id: UUID,
    val modulePath: String?,
    val metricType: String,
    val value: Double,
    val details: String?,
    val computedAt: Instant,
)

// Reports
data class ReportSummaryResponse(
    val id: UUID,
    val runId: UUID,
    val summary: String?,
    val riskScore: Double?,
    val createdAt: Instant,
)

data class ReportDetailResponse(
    val id: UUID,
    val runId: UUID,
    val repoId: UUID,
    val markdown: String,
    val summary: String?,
    val riskScore: Double?,
    val createdAt: Instant,
)

// Dashboard
data class DashboardResponse(
    val orgName: String,
    val plan: String,
    val repoCount: Int,
    val repos: List<RepoHealthSummary>,
)

data class RepoHealthSummary(
    val repoId: UUID,
    val name: String,
    val healthScore: Double?,
    val lastAnalyzedAt: Instant?,
)

data class RiskItem(
    val repoId: UUID,
    val repoName: String,
    val riskType: String,
    val severity: String,
    val description: String,
)

// Billing stub
data class BillingResponse(
    val plan: String,
    val repoLimit: Int,
    val repoCount: Int,
)

// Query (RAG stub)
data class QueryRequest(val question: String)
data class QueryResponse(
    val answer: String,
    val sources: List<String>,
)
