package dev.autopsy.db.repository

import dev.autopsy.db.entity.AnalysisRun
import java.util.UUID

interface AnalysisRunRepository {
    fun findById(id: UUID): AnalysisRun?
    fun findByRepoId(repoId: UUID): List<AnalysisRun>
    fun create(repoId: UUID): AnalysisRun
    fun updateStatus(id: UUID, status: String, stage: String?, error: String?): Unit
    fun updateCommitRange(id: UUID, commitRange: String): Unit
    fun markCompleted(id: UUID): Unit
}
