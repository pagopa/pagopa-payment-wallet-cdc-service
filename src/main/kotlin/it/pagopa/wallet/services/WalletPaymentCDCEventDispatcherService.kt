package it.pagopa.wallet.services

import com.azure.core.http.rest.Response
import com.azure.storage.queue.models.SendMessageResult
import it.pagopa.wallet.client.WalletQueueClient
import it.pagopa.wallet.common.tracing.TracingUtils
import it.pagopa.wallet.config.properties.ExpirationCdcQueueConfig
import java.time.Duration
import org.bson.BsonDocument
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class WalletPaymentCDCEventDispatcherService(
    private val walletQueueClient: WalletQueueClient,
    private val tracingUtils: TracingUtils,
    private val ExpirationCdcQueueConfig: ExpirationCdcQueueConfig,
) {

    private val WALLET_CDC_EVENT_HANDLER_SPAN_NAME = "cdcWalletEvent"
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val walletExpireTimeout =
        Duration.ofSeconds(ExpirationCdcQueueConfig.timeoutWalletExpired)

    fun dispatchEvent(event: BsonDocument): Mono<BsonDocument> =
        if (event != null) {
            onWalletEvent(event).map { event }
        } else {
            Mono.empty()
        }

    private fun onWalletEvent(event: BsonDocument): Mono<Response<SendMessageResult>> =
        tracingUtils
            .traceMonoQueue(WALLET_CDC_EVENT_HANDLER_SPAN_NAME) { tracingInfo ->
                walletQueueClient.sendWalletCreatedEvent(
                    event = event,
                    delay = walletExpireTimeout,
                    tracingInfo = tracingInfo
                )
            }
            .doOnError { logger.error("Failed to publish event") }
}
