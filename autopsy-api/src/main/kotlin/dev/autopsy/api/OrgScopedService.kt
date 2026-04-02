package dev.autopsy.api

import dev.autopsy.db.entity.AnalysisRun
import dev.autopsy.db.entity.RepoEntity
import dev.autopsy.db.entity.Report
import dev.autopsy.db.repository.AnalysisRunRepository
import dev.autopsy.db.repository.RepoRepository
import dev.autopsy.db.repository.ReportRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrgScopedService(
    private val repoRepo: RepoRepository,
    private val runRepo: AnalysisRunRepository,
    private val reportRepo: ReportRepository,
) {

    fun getRepoOrThrow(repoId: UUID, orgId: UUID): RepoEntity {
        val repo = repoRepo.findById(repoId)
            ?: throw NoSuchElementException("Repository not found: $repoId")
        if (repo.orgId != orgId) {
            throw NoSuchElementException("Repository not found: $repoId")
        }
        return repo
    }

    fun getRunOrThrow(runId: UUID, orgId: UUID): AnalysisRun {
        val run = runRepo.findById(runId)
            ?: throw NoSuchElementException("Analysis run not found: $runId")
        getRepoOrThrow(run.repoId, orgId)
        return run
    }

    fun getReportOrThrow(reportId: UUID, orgId: UUID): Report {
        val report = reportRepo.findById(reportId)
            ?: throw NoSuchElementException("Report not found: $reportId")
        getRepoOrThrow(report.repoId, orgId)
        return report
    }
}
