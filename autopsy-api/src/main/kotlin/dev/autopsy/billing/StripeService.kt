package dev.autopsy.billing

import com.stripe.Stripe
import com.stripe.model.Event
import com.stripe.model.checkout.Session
import com.stripe.net.Webhook
import com.stripe.param.checkout.SessionCreateParams
import dev.autopsy.db.entity.Organization
import dev.autopsy.db.repository.OrganizationRepository
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class StripeService(
    @Value("\${autopsy.stripe.secret-key:}") private val secretKey: String,
    @Value("\${autopsy.stripe.webhook-secret:}") private val webhookSecret: String,
    @Value("\${autopsy.stripe.success-url}") private val successUrl: String,
    @Value("\${autopsy.stripe.cancel-url}") private val cancelUrl: String,
    private val orgRepo: OrganizationRepository,
) {

    @PostConstruct
    fun init() {
        if (secretKey.isNotBlank()) {
            Stripe.apiKey = secretKey
        }
    }

    fun createCheckoutSession(org: Organization, priceId: String): String {
        val customerId = org.stripeCustomer ?: createAndSaveCustomer(org)

        val params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setCustomer(customerId)
            .setSuccessUrl(successUrl)
            .setCancelUrl(cancelUrl)
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setPrice(priceId)
                    .setQuantity(1)
                    .build(),
            )
            .build()

        val session = Session.create(params)
        return session.url
    }

    fun constructWebhookEvent(payload: String, sigHeader: String): Event {
        return Webhook.constructEvent(payload, sigHeader, webhookSecret)
    }

    private fun createAndSaveCustomer(org: Organization): String {
        val params = com.stripe.param.CustomerCreateParams.builder()
            .setName(org.name)
            .build()
        val customer = com.stripe.model.Customer.create(params)
        orgRepo.updateStripeCustomer(org.id, customer.id)
        return customer.id
    }
}
