package dev.autopsy.api

import com.stripe.model.Event
import com.stripe.model.Subscription
import dev.autopsy.billing.PlanLimits
import dev.autopsy.billing.StripeService
import dev.autopsy.db.repository.OrganizationRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/webhooks")
class StripeWebhookController(
    private val stripeService: StripeService,
    private val orgRepo: OrganizationRepository,
    private val planLimits: PlanLimits,
) {
    private val logger = LoggerFactory.getLogger(StripeWebhookController::class.java)

    @PostMapping("/stripe")
    fun handleWebhook(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") sigHeader: String,
    ): ResponseEntity<String> {
        val event: Event = try {
            stripeService.constructWebhookEvent(payload, sigHeader)
        } catch (e: Exception) {
            logger.warn("Webhook signature verification failed: {}", e.message)
            return ResponseEntity.badRequest().body("Invalid signature")
        }

        when (event.type) {
            "checkout.session.completed" -> handleCheckoutCompleted(event)
            "customer.subscription.updated" -> handleSubscriptionUpdated(event)
            "customer.subscription.deleted" -> handleSubscriptionDeleted(event)
            else -> logger.info("Unhandled webhook event: {}", event.type)
        }

        return ResponseEntity.ok("OK")
    }

    private fun handleCheckoutCompleted(event: Event) {
        val session = event.dataObjectDeserializer.`object`.orElse(null) ?: return
        val data = session as? com.stripe.model.checkout.Session ?: return
        val customerId = data.customer ?: return
        val subscriptionId = data.subscription ?: return

        val org = orgRepo.findByStripeCustomer(customerId) ?: run {
            logger.warn("No org found for Stripe customer: {}", customerId)
            return
        }

        // Fetch subscription to get price ID
        val subscription = Subscription.retrieve(subscriptionId)
        val priceId = subscription.items.data.firstOrNull()?.price?.id ?: return
        val plan = planLimits.planForPriceId(priceId) ?: "pro"
        val repoLimit = PlanLimits.repoLimitForPlan(plan)

        orgRepo.updatePlan(requireNotNull(org.id) { "Organization ID must not be null" }, plan, repoLimit, subscriptionId, priceId)
        logger.info("Org {} upgraded to {} via checkout", org.id, plan)
    }

    private fun handleSubscriptionUpdated(event: Event) {
        val subscription = event.dataObjectDeserializer.`object`.orElse(null) as? Subscription ?: return
        val customerId = subscription.customer ?: return

        val org = orgRepo.findByStripeCustomer(customerId) ?: return

        val priceId = subscription.items.data.firstOrNull()?.price?.id ?: return
        val plan = planLimits.planForPriceId(priceId) ?: return
        val repoLimit = PlanLimits.repoLimitForPlan(plan)

        orgRepo.updatePlan(requireNotNull(org.id) { "Organization ID must not be null" }, plan, repoLimit, subscription.id, priceId)
        logger.info("Org {} plan updated to {}", org.id, plan)
    }

    private fun handleSubscriptionDeleted(event: Event) {
        val subscription = event.dataObjectDeserializer.`object`.orElse(null) as? Subscription ?: return
        val customerId = subscription.customer ?: return

        val org = orgRepo.findByStripeCustomer(customerId) ?: return

        orgRepo.updatePlan(requireNotNull(org.id) { "Organization ID must not be null" }, "free", PlanLimits.repoLimitForPlan("free"), null, null)
        logger.info("Org {} downgraded to free", org.id)
    }
}
