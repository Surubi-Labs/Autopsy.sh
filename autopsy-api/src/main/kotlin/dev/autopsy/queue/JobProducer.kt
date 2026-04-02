package dev.autopsy.queue

import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class JobProducer(
    private val redisTemplate: StringRedisTemplate,
) {
    companion object {
        const val STREAM_KEY = "autopsy:jobs"
    }

    /**
     * Enqueue a job message to the Redis stream.
     * Returns the stream entry ID.
     */
    fun enqueue(message: JobMessage): String {
        val record: MapRecord<String, String, String> = StreamRecords
            .mapBacked<String, String, String>(message.toMap())
            .withStreamKey(STREAM_KEY)

        val recordId = redisTemplate.opsForStream<String, String>().add(record)
        return recordId?.value ?: ""
    }

    /**
     * Enqueue the next stage in the pipeline for a given run.
     * Returns null if there is no next stage.
     */
    fun enqueueNextStage(
        runId: String,
        repoId: String,
        orgId: String,
        currentStage: Stage,
        payload: Map<String, String> = emptyMap(),
    ): String? {
        val nextStage = currentStage.next() ?: return null
        val message = JobMessage(
            runId = runId,
            repoId = repoId,
            orgId = orgId,
            stage = nextStage,
            payload = payload,
        )
        return enqueue(message)
    }
}
