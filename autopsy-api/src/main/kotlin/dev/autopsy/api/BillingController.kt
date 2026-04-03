package dev.autopsy.api

import dev.autopsy.api.dto.BillingResponse
import dev.autopsy.auth.AuthenticatedOrg
import dev.autopsy.billing.PlanLimits
import dev.autopsy.billing.StripeService
import dev.autopsy.db.repository.OrganizationRepository
import dev.autopsy.db.repository.RepoRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/billing")
class BillingController(
    private val orgRepo: OrganizationRepository,
    private val repoRepo: RepoRepository,
    private val stripeService: StripeService,
    private val planLimits: PlanLimits,
) {

    @GetMapping
    fun getBilling(@AuthenticationPrincipal auth: AuthenticatedOrg): BillingResponse {
        val org = orgRepo.findById(auth.orgId)
        val repoCount = repoRepo.countByOrgId(auth.orgId)
        return BillingResponse(
            plan = org?.plan ?: "free",
            repoLimit = org?.repoLimit ?: 1,
            repoCount = repoCount,
        )
    }

    @PostMapping("/checkout")
    fun createCheckout(
        @AuthenticationPrincipal auth: AuthenticatedOrg,
        @RequestBody request: CheckoutRequest,
    ): ResponseEntity<CheckoutResponse> {
        val org = orgRepo.findById(auth.orgId)
            ?: return ResponseEntity.notFound().build()

        val plan = planLimits.planForPriceId(request.priceId)
            ?: return ResponseEntity.badRequest().build()

        val url = stripeService.createCheckoutSession(org, request.priceId)
        return ResponseEntity.ok(CheckoutResponse(url))
    }
}

data class CheckoutRequest(val priceId: String)
data class CheckoutResponse(val url: String)
