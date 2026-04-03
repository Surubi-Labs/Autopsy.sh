package dev.autopsy.pipeline

import dev.autopsy.db.entity.AnalysisRun
import dev.autopsy.db.entity.RepoEntity
import dev.autopsy.db.repository.AnalysisRunRepository
import dev.autopsy.db.repository.RepoRepository
import dev.autopsy.queue.JobProducer
import dev.autopsy.queue.Stage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class PipelineOrchestratorTest {

    private val runRepo: AnalysisRunRepository = mock()
    private val repoRepo: RepoRepository = mock()
    private val orgRepo: dev.autopsy.db.repository.OrganizationRepository = mock()
    private val jobProducer: JobProducer = mock()
    private val orchestrator = PipelineOrchestrator(runRepo, repoRepo, orgRepo, jobProducer)

    private val orgId = UUID.randomUUID()
    private val repoId = UUID.randomUUID()
    private val runId = UUID.randomUUID()

    private val testRepo = RepoEntity(
        id = repoId, orgId = orgId, name = "test-repo",
        gitUrl = "https://github.com/org/repo.git", defaultBranch = "main",
        githubInstallId = null, lastAnalyzedSha = null, lastAnalyzedAt = null,
        schedule = "weekly", active = true, createdAt = Instant.now(),
    )

    private val testRun = AnalysisRun(
        id = runId, repoId = repoId, status = "queued",
        currentStage = null, startedAt = null, completedAt = null,
        error = null, commitRange = null, createdAt = Instant.now(),
    )

    @Test
    fun `triggerAnalysis creates run and enqueues CLONE`() {
        whenever(repoRepo.findById(repoId)).thenReturn(testRepo)
        whenever(runRepo.create(repoId)).thenReturn(testRun)
        whenever(jobProducer.enqueue(any())).thenReturn("1234-0")

        val result = orchestrator.triggerAnalysis(repoId)

        assertEquals(runId, result)
        verify(runRepo).create(repoId)
        verify(jobProducer).enqueue(argThat {
            this.stage == Stage.CLONE && this.runId == runId.toString()
        })
    }

    @Test
    fun `triggerAnalysis throws for unknown repo`() {
        whenever(repoRepo.findById(any())).thenReturn(null)

        assertThrows<IllegalArgumentException> {
            orchestrator.triggerAnalysis(UUID.randomUUID())
        }
    }

    @Test
    fun `triggerAnalysisForOrg triggers all active repos`() {
        val repo2Id = UUID.randomUUID()
        val repo2 = testRepo.copy(id = repo2Id)
        whenever(repoRepo.findActiveByOrgId(orgId)).thenReturn(listOf(testRepo, repo2))
        whenever(repoRepo.findById(any())).thenReturn(testRepo)
        whenever(runRepo.create(any())).thenReturn(testRun)
        whenever(jobProducer.enqueue(any())).thenReturn("1234-0")

        val results = orchestrator.triggerAnalysisForOrg(orgId)

        assertEquals(2, results.size)
        verify(jobProducer, times(2)).enqueue(any())
    }
}
