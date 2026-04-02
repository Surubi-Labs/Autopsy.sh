package dev.autopsy.api

import dev.autopsy.api.dto.PageResponse
import dev.autopsy.api.dto.ReportDetailResponse
import dev.autopsy.api.dto.ReportSummaryResponse
import dev.autopsy.api.dto.toDetailResponse
import dev.autopsy.api.dto.toSummaryResponse
import dev.autopsy.auth.AuthenticatedOrg
import dev.autopsy.db.repository.ReportRepository
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
class ReportController(
    private val reportRepo: ReportRepository,
    private val orgScoped: OrgScopedService,
) {

    @GetMapping("/repos/{id}/reports")
    fun listReports(
        @AuthenticationPrincipal auth: AuthenticatedOrg,
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int,
    ): PageResponse<ReportSummaryResponse> {
        orgScoped.getRepoOrThrow(id, auth.orgId)
        val reports = reportRepo.findByRepoIdPaginated(id, limit, offset).map { it.toSummaryResponse() }
        val total = reportRepo.countByRepoId(id)
        return PageResponse(reports, total, limit, offset)
    }

    @GetMapping("/reports/{id}")
    fun getReportDetail(
        @AuthenticationPrincipal auth: AuthenticatedOrg,
        @PathVariable id: UUID,
    ): ReportDetailResponse {
        val report = orgScoped.getReportOrThrow(id, auth.orgId)
        return report.toDetailResponse()
    }
}
