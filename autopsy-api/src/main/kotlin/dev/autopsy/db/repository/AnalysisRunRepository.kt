package dev.autopsy.db.repository

import dev.autopsy.db.entity.AnalysisRun
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface AnalysisRunRepository : CrudRepository<AnalysisRun, UUID> {

    @Query("SELECT * FROM analysis_runs WHERE repo_id = :repoId ORDER BY created_at DESC")
    fun findByRepoId(repoId: UUID): List<AnalysisRun>

    @Modifying
    @Query("""
        UPDATE analysis_runs
        SET status = :status, current_stage = :stage, error = :error,
            started_at = CASE WHEN started_at IS NULL AND :status = 'running' THEN now() ELSE started_at END
        WHERE id = :id
    """)
    fun updateStatus(id: UUID, status: String, stage: String?, error: String?)

    @Modifying
    @Query("UPDATE analysis_runs SET commit_range = :commitRange WHERE id = :id")
    fun updateCommitRange(id: UUID, commitRange: String)

    @Modifying
    @Query("UPDATE analysis_runs SET status = 'completed', completed_at = now() WHERE id = :id")
    fun markCompleted(id: UUID)

    @Query("SELECT * FROM analysis_runs WHERE repo_id = :repoId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    fun findByRepoIdPaginated(repoId: UUID, limit: Int, offset: Int): List<AnalysisRun>

    @Query("SELECT COUNT(*) FROM analysis_runs WHERE repo_id = :repoId")
    fun countByRepoId(repoId: UUID): Int
}
