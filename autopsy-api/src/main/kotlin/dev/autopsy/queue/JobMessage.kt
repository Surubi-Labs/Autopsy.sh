package dev.autopsy.queue

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class JobMessage(
    val runId: String,
    val repoId: String,
    val orgId: String,
    val stage: Stage,
    val payload: Map<String, String> = emptyMap(),
    val priority: Int = 0,
) {
    fun toMap(): Map<String, String> = buildMap {
        put("runId", runId)
        put("repoId", repoId)
        put("orgId", orgId)
        put("stage", stage.name)
        if (payload.isNotEmpty()) {
            put("payload", MAPPER.writeValueAsString(payload))
        }
        if (priority > 0) {
            put("priority", priority.toString())
        }
    }

    companion object {
        private val MAPPER = jacksonObjectMapper()

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, String>): JobMessage = JobMessage(
            runId = map.getValue("runId"),
            repoId = map.getValue("repoId"),
            orgId = map.getValue("orgId"),
            stage = Stage.valueOf(map.getValue("stage")),
            payload = map["payload"]?.let {
                MAPPER.readValue(it, Map::class.java) as Map<String, String>
            } ?: emptyMap(),
            priority = map["priority"]?.toIntOrNull() ?: 0,
        )
    }
}

enum class Stage {
    CLONE, EXTRACT, METRICS, EMBED, REPORT, DELIVER;

    fun next(): Stage? {
        val values = entries
        val nextOrdinal = ordinal + 1
        return if (nextOrdinal < values.size) values[nextOrdinal] else null
    }
}
