package dev.autopsy.api

import dev.autopsy.api.dto.ConnectRepoRequest
import dev.autopsy.api.dto.RepoResponse
import dev.autopsy.api.dto.toResponse
import dev.autopsy.auth.AuthenticatedOrg
import dev.autopsy.db.entity.RepoEntity
import dev.autopsy.db.repository.OrganizationRepository
import dev.autopsy.db.repository.RepoRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/repos")
class RepositoryController(
    private val repoRepo: RepoRepository,
    private val orgRepo: OrganizationRepository,
    private val orgScoped: OrgScopedService,
) {

    @GetMapping
    fun listRepos(@AuthenticationPrincipal auth: AuthenticatedOrg): List<RepoResponse> {
        return repoRepo.findByOrgId(auth.orgId).map { it.toResponse() }
    }

    @PostMapping
    fun connectRepo(
        @AuthenticationPrincipal auth: AuthenticatedOrg,
        @RequestBody request: ConnectRepoRequest,
    ): ResponseEntity<RepoResponse> {
        val org = orgRepo.findByIdOrNull(auth.orgId)
            ?: return ResponseEntity.notFound().build()

        val currentCount = repoRepo.countByOrgId(auth.orgId)
        if (currentCount >= org.repoLimit) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val repo = repoRepo.save(
            RepoEntity(orgId = auth.orgId, name = request.name, gitUrl = request.gitUrl, defaultBranch = request.defaultBranch),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(repo.toResponse())
    }

    @DeleteMapping("/{id}")
    fun deleteRepo(
        @AuthenticationPrincipal auth: AuthenticatedOrg,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        orgScoped.getRepoOrThrow(id, auth.orgId)
        repoRepo.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
