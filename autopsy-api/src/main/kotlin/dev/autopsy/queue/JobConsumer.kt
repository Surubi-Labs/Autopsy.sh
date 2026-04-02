package dev.autopsy.queue

import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class JobConsumer(
    private val redisTemplate: StringRedisTemplate,
) {
    private val logger = LoggerFactory.getLogger(JobConsumer::class.java)

    companion object {
        const val STREAM_KEY = "autopsy:jobs"
        const val GROUP_NAME = "autopsy-kotlin"
        const val CONSUMER_NAME = "metrics-worker"
    }

    /**
     * Ensure the consumer group exists. Idempotent.
     */
    fun ensureGroup() {
        try {
            redisTemplate.opsForStream<String, String>().createGroup(STREAM_KEY, GROUP_NAME)
        } catch (_: Exception) {
            // Group already exists — safe to ignore
        }
    }

    /**
     * Read one message from the stream for this consumer group.
     * Returns null if no message is available within the timeout.
     */
    fun readOne(blockTimeout: Duration = Duration.ofSeconds(5)): Pair<String, JobMessage>? {
        val messages: List<MapRecord<String, String, String>>? = redisTemplate
            .opsForStream<String, String>()
            .read(
                Consumer.from(GROUP_NAME, CONSUMER_NAME),
                org.springframework.data.redis.connection.stream.StreamReadOptions.empty()
                    .count(1)
                    .block(blockTimeout),
                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()),
            )

        val record = messages?.firstOrNull() ?: return null
        val jobMessage = JobMessage.fromMap(record.value)
        return record.id.value to jobMessage
    }

    /**
     * Acknowledge a message as processed.
     */
    fun ack(messageId: String) {
        redisTemplate.opsForStream<String, String>().acknowledge(STREAM_KEY, GROUP_NAME, messageId)
    }
}
