package it.pagopa.wallet.services

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import it.pagopa.wallet.client.WalletQueueClient
import it.pagopa.wallet.common.tracing.TracingUtils
import it.pagopa.wallet.config.properties.CdcQueueConfig
import it.pagopa.wallet.config.properties.RetrySendPolicyConfig
import java.time.Duration
import org.bson.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

@Component
class WalletPaymentCDCEventDispatcherService(
    private val walletQueueClient: WalletQueueClient,
    private val tracingUtils: TracingUtils,
    private val cdcQueueConfig: CdcQueueConfig,
    private val retrySendPolicyConfig: RetrySendPolicyConfig
) {

    private val WALLET_CDC_EVENT_HANDLER_SPAN_NAME = "cdcWalletEvent"
    private val WALLET_CDC_EVENT_ENRICHMENT_SPAN_NAME = "cdcWalletEventEnrichment"
    private val WALLET_CDC_EVENT_TYPE = AttributeKey.stringKey("paymentWallet.ingestion.eventType")
    private val WALLET_CDC_EVENT_WALLET_ID =
        AttributeKey.stringKey("paymentWallet.ingestion.walletId")
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun dispatchEvent(event: Document?): Mono<Document> =
        if (event != null) {
            Mono.defer {
                    logger.info(
                        "Handling new change stream event of type {} for wallet with id {} published on {}",
                        event["_class"],
                        event["walletId"],
                        event["timestamp"]
                    )
                    tracingUtils.traceMonoQueue(WALLET_CDC_EVENT_HANDLER_SPAN_NAME) { tracingInfo ->
                        tracingUtils.addSpan(
                            WALLET_CDC_EVENT_ENRICHMENT_SPAN_NAME,
                            getSpanAttributes(event)
                        )
                        walletQueueClient.sendWalletEvent(
                            event = event,
                            delay = Duration.ofSeconds(cdcQueueConfig.visibilityTimeoutWalletCdc),
                            tracingInfo = tracingInfo,
                        )
                    }
                }
                .retryWhen(
                    Retry.fixedDelay(
                            retrySendPolicyConfig.maxAttempts,
                            Duration.ofMillis(retrySendPolicyConfig.intervalInMs)
                        )
                        .filter { t -> t is Exception }
                        .doBeforeRetry { signal ->
                            logger.warn(
                                "Retrying writing event on CDC queue due to: ${signal.failure().message}"
                            )
                        }
                )
                .doOnError { e -> logger.error("Failed to send event after retries {}", e.message) }
                .map { event }
        } else {
            Mono.empty()
        }

    private fun getSpanAttributes(event: Document) =
        Attributes.of(
            WALLET_CDC_EVENT_WALLET_ID,
            event["walletId"].toString(),
            WALLET_CDC_EVENT_TYPE,
            event["_class"].toString().split(".").last()
        )
}
