package dev.autopsy.db.repository

import dev.autopsy.db.entity.ChunkEntity
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface ChunkRepository : CrudRepository<ChunkEntity, UUID> {
    fun findByRepoId(repoId: UUID): List<ChunkEntity>
    fun deleteByRepoId(repoId: UUID): Int
}
