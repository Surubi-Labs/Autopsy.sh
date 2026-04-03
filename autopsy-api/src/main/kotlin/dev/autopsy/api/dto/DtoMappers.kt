package dev.autopsy.api.dto

import dev.autopsy.db.entity.AnalysisRun
import dev.autopsy.db.entity.Metric
import dev.autopsy.db.entity.RepoEntity
import dev.autopsy.db.entity.Report
import java.time.Instant

fun RepoEntity.toResponse(): RepoResponse = RepoResponse(
    id = requireNotNull(id) { "RepoEntity ID must not be null" },
    name = name,
    gitUrl = gitUrl,
    defaultBranch = defaultBranch,
    schedule = schedule,
    active = active,
    lastAnalyzedSha = lastAnalyzedSha,
    lastAnalyzedAt = lastAnalyzedAt,
    createdAt = createdAt ?: Instant.now(),
)

fun AnalysisRun.toResponse(): RunResponse = RunResponse(
    id = requireNotNull(id) { "AnalysisRun ID must not be null" },
    repoId = repoId,
    status = status,
    currentStage = currentStage,
    startedAt = startedAt,
    completedAt = completedAt,
    error = error,
    commitRange = commitRange,
    createdAt = createdAt ?: Instant.now(),
)

fun Metric.toResponse(): MetricResponse = MetricResponse(
    id = requireNotNull(id) { "Metric ID must not be null" },
    modulePath = modulePath,
    metricType = metricType,
    value = value,
    details = details,
    computedAt = computedAt ?: Instant.now(),
)

fun Report.toSummaryResponse(): ReportSummaryResponse = ReportSummaryResponse(
    id = requireNotNull(id) { "Report ID must not be null" },
    runId = runId,
    summary = summary,
    riskScore = riskScore,
    createdAt = createdAt ?: Instant.now(),
)

fun Report.toDetailResponse(): ReportDetailResponse = ReportDetailResponse(
    id = requireNotNull(id) { "Report ID must not be null" },
    runId = runId,
    repoId = repoId,
    markdown = markdown,
    summary = summary,
    riskScore = riskScore,
    createdAt = createdAt ?: Instant.now(),
)
