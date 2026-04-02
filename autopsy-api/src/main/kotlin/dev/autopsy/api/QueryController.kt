package dev.autopsy.api

import dev.autopsy.api.dto.QueryRequest
import dev.autopsy.api.dto.QueryResponse
import dev.autopsy.auth.AuthenticatedOrg
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
class QueryController(
    private val orgScoped: OrgScopedService,
) {

    @PostMapping("/repos/{id}/query")
    fun query(
        @AuthenticationPrincipal auth: AuthenticatedOrg,
        @PathVariable id: UUID,
        @RequestBody request: QueryRequest,
    ): QueryResponse {
        orgScoped.getRepoOrThrow(id, auth.orgId)
        return QueryResponse(
            answer = "RAG query not yet implemented. Your question: ${request.question}",
            sources = emptyList(),
        )
    }
}
