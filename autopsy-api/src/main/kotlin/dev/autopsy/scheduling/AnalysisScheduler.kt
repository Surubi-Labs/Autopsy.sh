package dev.autopsy.scheduling

import dev.autopsy.db.repository.RepoRepository
import dev.autopsy.pipeline.PipelineOrchestrator
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class AnalysisScheduler(
    private val repoRepo: RepoRepository,
    private val orchestrator: PipelineOrchestrator,
) {
    private val logger = LoggerFactory.getLogger(AnalysisScheduler::class.java)

    @Scheduled(cron = "0 0 2 * * MON")
    fun triggerWeeklyAnalysis() {
        val repos = repoRepo.findActiveBySchedule("weekly")
        logger.info("Weekly analysis: triggering {} repos", repos.size)
        for (repo in repos) {
            try {
                orchestrator.triggerAnalysis(requireNotNull(repo.id) { "RepoEntity ID must not be null" })
            } catch (e: Exception) {
                logger.error("Failed to trigger analysis for repo {}: {}", repo.id, e.message)
            }
        }
        logger.info("Weekly analysis: done")
    }

    @Scheduled(cron = "0 0 2 * * *")
    fun triggerDailyAnalysis() {
        val repos = repoRepo.findActiveBySchedule("daily")
        logger.info("Daily analysis: triggering {} repos", repos.size)
        for (repo in repos) {
            try {
                orchestrator.triggerAnalysis(requireNotNull(repo.id) { "RepoEntity ID must not be null" })
            } catch (e: Exception) {
                logger.error("Failed to trigger analysis for repo {}: {}", repo.id, e.message)
            }
        }
        logger.info("Daily analysis: done")
    }
}
