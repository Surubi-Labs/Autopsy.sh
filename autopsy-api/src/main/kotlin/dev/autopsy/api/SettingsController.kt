package dev.autopsy.api

import dev.autopsy.auth.AuthenticatedOrg
import dev.autopsy.db.repository.OrganizationRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/settings")
class SettingsController(
    private val orgRepo: OrganizationRepository,
) {

    @GetMapping("/delivery")
    fun getDeliveryPreferences(
        @AuthenticationPrincipal auth: AuthenticatedOrg,
    ): DeliveryPreferencesResponse {
        val org = orgRepo.findByIdOrNull(auth.orgId)
        return DeliveryPreferencesResponse(
            emailAddress = org?.emailAddress,
            slackWebhookUrl = org?.slackWebhookUrl,
        )
    }

    @PutMapping("/delivery")
    fun updateDeliveryPreferences(
        @AuthenticationPrincipal auth: AuthenticatedOrg,
        @RequestBody request: DeliveryPreferencesRequest,
    ): ResponseEntity<DeliveryPreferencesResponse> {
        if (request.slackWebhookUrl != null && !isValidSlackWebhook(request.slackWebhookUrl)) {
            return ResponseEntity.badRequest().build()
        }

        orgRepo.updateDeliveryPreferences(auth.orgId, request.emailAddress, request.slackWebhookUrl)

        return ResponseEntity.ok(
            DeliveryPreferencesResponse(
                emailAddress = request.emailAddress,
                slackWebhookUrl = request.slackWebhookUrl,
            ),
        )
    }

    private fun isValidSlackWebhook(url: String): Boolean =
        url.startsWith("https://hooks.slack.com/services/")
}

data class DeliveryPreferencesRequest(
    val emailAddress: String? = null,
    val slackWebhookUrl: String? = null,
)

data class DeliveryPreferencesResponse(
    val emailAddress: String?,
    val slackWebhookUrl: String?,
)
