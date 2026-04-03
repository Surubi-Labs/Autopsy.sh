package dev.autopsy.pipeline

import dev.autopsy.queue.JobConsumer
import dev.autopsy.queue.Stage
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Starts the Kotlin metrics consumer when running in server mode.
 * Consumes METRICS stage jobs from Redis and delegates to MetricsStageHandler.
 */
@Component
@Profile("!cli")
class MetricsConsumerRunner(
    private val jobConsumer: JobConsumer,
    private val metricsHandler: MetricsStageHandler,
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(MetricsConsumerRunner::class.java)

    @Volatile
    private var running = true

    override fun run(args: ApplicationArguments) {
        Thread({
            logger.info("Starting metrics consumer...")
            jobConsumer.ensureGroup()

            while (running) {
                try {
                    val result = jobConsumer.readOne() ?: continue
                    val (messageId, message) = result

                    if (message.stage != Stage.METRICS) {
                        // Not for us — re-enqueue or skip
                        // In practice, consumer groups handle this via filtering
                        jobConsumer.ack(messageId)
                        continue
                    }

                    logger.info("Processing METRICS for run {}", message.runId)
                    metricsHandler.handle(
                        UUID.fromString(message.runId),
                        UUID.fromString(message.repoId),
                        UUID.fromString(message.orgId),
                    )
                    jobConsumer.ack(messageId)

                } catch (e: Exception) {
                    if (Thread.currentThread().isInterrupted) break
                    logger.error("Error processing metrics job", e)
                    // Don't ack — message stays pending for retry
                }
            }
        }, "metrics-consumer").apply { isDaemon = true }.start()
    }

    @PreDestroy
    fun stop() {
        running = false
    }
}
