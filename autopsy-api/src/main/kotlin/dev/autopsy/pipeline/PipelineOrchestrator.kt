package dev.autopsy.pipeline

import dev.autopsy.billing.PlanLimits
import dev.autopsy.db.repository.AnalysisRunRepository
import dev.autopsy.db.repository.OrganizationRepository
import dev.autopsy.db.repository.RepoRepository
import dev.autopsy.queue.JobMessage
import dev.autopsy.queue.JobProducer
import dev.autopsy.queue.Stage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PipelineOrchestrator(
    private val runRepository: AnalysisRunRepository,
    private val repoRepository: RepoRepository,
    private val orgRepository: OrganizationRepository,
    private val jobProducer: JobProducer,
) {
    private val logger = LoggerFactory.getLogger(PipelineOrchestrator::class.java)

    /**
     * Trigger analysis for a single repository.
     * Creates a new analysis run and enqueues the first pipeline stage.
     */
    fun triggerAnalysis(repoId: UUID): UUID {
        val repo = repoRepository.findById(repoId)
            ?: throw IllegalArgumentException("Repository not found: $repoId")

        val run = runRepository.create(repoId)

        val org = orgRepository.findById(repo.orgId)
        val priority = if (org?.plan == "team") 1 else 0

        jobProducer.enqueue(
            JobMessage(
                runId = run.id.toString(),
                repoId = repoId.toString(),
                orgId = repo.orgId.toString(),
                stage = Stage.CLONE,
                priority = priority,
            ),
        )

        logger.info("Triggered analysis run {} for repo {}", run.id, repo.name)
        return run.id
    }

    /**
     * Trigger analysis for all active repositories in an organization.
     */
    fun triggerAnalysisForOrg(orgId: UUID): List<UUID> {
        val repos = repoRepository.findActiveByOrgId(orgId)
        return repos.map { repo -> triggerAnalysis(repo.id) }
    }
}
