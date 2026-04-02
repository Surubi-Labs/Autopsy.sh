package dev.autopsy.db.repository

import dev.autopsy.db.entity.ChunkEntity
import java.util.UUID

interface ChunkRepository {
    fun findByRepoId(repoId: UUID): List<ChunkEntity>
    fun deleteByRepoId(repoId: UUID): Int
    fun save(repoId: UUID, sourceType: String, sourceRef: String, content: String): ChunkEntity
}
