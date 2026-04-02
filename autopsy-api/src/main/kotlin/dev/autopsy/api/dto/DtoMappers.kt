package dev.autopsy.api.dto

import dev.autopsy.db.entity.AnalysisRun
import dev.autopsy.db.entity.Metric
import dev.autopsy.db.entity.RepoEntity
import dev.autopsy.db.entity.Report

fun RepoEntity.toResponse(): RepoResponse = RepoResponse(
    id = id,
    name = name,
    gitUrl = gitUrl,
    defaultBranch = defaultBranch,
    schedule = schedule,
    active = active,
    lastAnalyzedSha = lastAnalyzedSha,
    lastAnalyzedAt = lastAnalyzedAt,
    createdAt = createdAt,
)

fun AnalysisRun.toResponse(): RunResponse = RunResponse(
    id = id,
    repoId = repoId,
    status = status,
    currentStage = currentStage,
    startedAt = startedAt,
    completedAt = completedAt,
    error = error,
    commitRange = commitRange,
    createdAt = createdAt,
)

fun Metric.toResponse(): MetricResponse = MetricResponse(
    id = id,
    modulePath = modulePath,
    metricType = metricType,
    value = value,
    details = details,
    computedAt = computedAt,
)

fun Report.toSummaryResponse(): ReportSummaryResponse = ReportSummaryResponse(
    id = id,
    runId = runId,
    summary = summary,
    riskScore = riskScore,
    createdAt = createdAt,
)

fun Report.toDetailResponse(): ReportDetailResponse = ReportDetailResponse(
    id = id,
    runId = runId,
    repoId = repoId,
    markdown = markdown,
    summary = summary,
    riskScore = riskScore,
    createdAt = createdAt,
)
