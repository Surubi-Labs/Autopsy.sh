package dev.autopsy.pipeline

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.autopsy.db.entity.Metric
import dev.autopsy.db.repository.AnalysisRunRepository
import dev.autopsy.db.repository.ExtractionRepository
import dev.autopsy.db.repository.MetricRepository
import dev.autopsy.extraction.CommitExtraction
import dev.autopsy.extraction.DependencyExtraction
import dev.autopsy.metrics.*
import dev.autopsy.queue.JobProducer
import dev.autopsy.queue.Stage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MetricsStageHandler(
    private val extractionRepo: ExtractionRepository,
    private val metricRepo: MetricRepository,
    private val runRepo: AnalysisRunRepository,
    private val jobProducer: JobProducer,
) {
    private val logger = LoggerFactory.getLogger(MetricsStageHandler::class.java)
    private val mapper = jacksonObjectMapper()

    fun handle(runId: UUID, repoId: UUID, orgId: UUID) {
        runRepo.updateStatus(runId, "running", "metrics", null)

        try {
            // Read extractions from DB
            val commitExtractions = extractionRepo.findByRunIdAndType(runId, "commit")
            val depExtractions = extractionRepo.findByRunIdAndType(runId, "dependency")
            val fileDiffExtractions = extractionRepo.findByRunIdAndType(runId, "file_diff")

            // Deserialize
            val commits = commitExtractions.map { mapper.readValue<CommitExtraction>(it.payload) }
            val dependencies = depExtractions.map { mapper.readValue<DependencyExtraction>(it.payload) }

            val testFiles = fileDiffExtractions.firstOrNull()?.let {
                val payload = mapper.readValue<Map<String, List<String>>>(it.payload)
                payload["test_files"] ?: emptyList()
            } ?: emptyList()

            val sourceFiles = fileDiffExtractions.firstOrNull()?.let {
                val payload = mapper.readValue<Map<String, List<String>>>(it.payload)
                payload["source_files"] ?: emptyList()
            } ?: emptyList()

            // Compute metrics using existing pure functions
            val modules = ModuleDetector.detectModules(commits)
            val churn = ChurnCalculator.calculateChurn(commits, modules)
            val busFactor = BusFactorCalculator.calculateBusFactor(commits, modules)
            val hotspots = HotspotDetector.detectHotspots(commits)
            val staleness = DependencyStalenessScorer.scoreStaleness(dependencies)
            val testCoverage = TestCoverageTrendTracker.trackTestCoverage(commits, testFiles, sourceFiles, modules)
            val healthScore = HealthScoreComputer.computeHealthScore(churn, busFactor, staleness, testCoverage, hotspots)

            // Write metrics to DB
            metricRepo.save(Metric(runId = runId, repoId = repoId, modulePath = null, metricType = "health_score",
                value = healthScore.overall, details = mapper.writeValueAsString(healthScore.breakdown)))

            for ((module, bf) in busFactor) {
                metricRepo.save(Metric(runId = runId, repoId = repoId, modulePath = module, metricType = "bus_factor",
                    value = bf.busFactor.toDouble(), details = mapper.writeValueAsString(bf)))
            }

            metricRepo.save(Metric(runId = runId, repoId = repoId, modulePath = null, metricType = "dep_staleness",
                value = staleness.overallScore, details = mapper.writeValueAsString(staleness)))

            for (hs in hotspots.take(10)) {
                metricRepo.save(Metric(runId = runId, repoId = repoId, modulePath = null, metricType = "hotspot",
                    value = hs.score, details = mapper.writeValueAsString(hs)))
            }

            logger.info("Computed metrics for run {}: health={}", runId, healthScore.overall)

            // Enqueue next stage
            jobProducer.enqueueNextStage(runId.toString(), repoId.toString(), orgId.toString(), Stage.METRICS)

        } catch (e: Exception) {
            runRepo.updateStatus(runId, "failed", "metrics", e.message)
            throw e
        }
    }
}
