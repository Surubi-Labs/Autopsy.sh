package dev.autopsy.api

import dev.autopsy.api.dto.PageResponse
import dev.autopsy.api.dto.RunResponse
import dev.autopsy.api.dto.TriggerAnalysisResponse
import dev.autopsy.api.dto.toResponse
import dev.autopsy.auth.AuthenticatedOrg
import dev.autopsy.db.repository.AnalysisRunRepository
import dev.autopsy.pipeline.PipelineOrchestrator
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
class AnalysisController(
    private val runRepo: AnalysisRunRepository,
    private val orchestrator: PipelineOrchestrator,
    private val orgScoped: OrgScopedService,
) {

    @PostMapping("/repos/{id}/analyze")
    fun triggerAnalysis(
        @AuthenticationPrincipal auth: AuthenticatedOrg,
        @PathVariable id: UUID,
    ): ResponseEntity<TriggerAnalysisResponse> {
        orgScoped.getRepoOrThrow(id, auth.orgId)
        val runId = orchestrator.triggerAnalysis(id)
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(TriggerAnalysisResponse(runId))
    }

    @GetMapping("/repos/{id}/runs")
    fun listRuns(
        @AuthenticationPrincipal auth: AuthenticatedOrg,
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int,
    ): PageResponse<RunResponse> {
        orgScoped.getRepoOrThrow(id, auth.orgId)
        val runs = runRepo.findByRepoIdPaginated(id, limit, offset).map { it.toResponse() }
        val total = runRepo.countByRepoId(id)
        return PageResponse(runs, total, limit, offset)
    }

    @GetMapping("/runs/{id}")
    fun getRunDetail(
        @AuthenticationPrincipal auth: AuthenticatedOrg,
        @PathVariable id: UUID,
    ): RunResponse {
        val run = orgScoped.getRunOrThrow(id, auth.orgId)
        return run.toResponse()
    }
}
