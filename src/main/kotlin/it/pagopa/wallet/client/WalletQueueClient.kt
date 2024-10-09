package it.pagopa.wallet.client

import com.azure.core.http.rest.Response
import com.azure.core.util.BinaryData
import com.azure.core.util.serializer.JsonSerializer
import com.azure.storage.queue.QueueAsyncClient
import com.azure.storage.queue.models.SendMessageResult
import it.pagopa.wallet.cdc.PaymentWalletsLogEventsStream
import it.pagopa.wallet.common.QueueEvent
import it.pagopa.wallet.common.tracing.QueueTracingInfo
import it.pagopa.wallet.config.RetrySendPolicyConfig
import java.time.Duration
import org.bson.BsonDocument
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

class WalletQueueClient(
    private val cdcQueueClient: QueueAsyncClient,
    private val jsonSerializer: JsonSerializer,
    private val ttl: Duration,
    @Autowired private val retrySendPolicyConfig: RetrySendPolicyConfig,
) {
    private val logger = LoggerFactory.getLogger(PaymentWalletsLogEventsStream::class.java)

    fun sendWalletEvent(
        event: BsonDocument,
        delay: Duration,
        tracingInfo: QueueTracingInfo
    ): Mono<Response<SendMessageResult>> {
        val queueEvent = QueueEvent(event, tracingInfo)
        return BinaryData.fromObjectAsync(queueEvent, jsonSerializer)
            .flatMap { cdcQueueClient.sendMessageWithResponse(it, delay, ttl) }
            .retryWhen(
                Retry.fixedDelay(
                        retrySendPolicyConfig.maxAttempts,
                        Duration.ofMillis(retrySendPolicyConfig.intervalInMs)
                    )
                    .filter { t -> t is Exception }
                    .doBeforeRetry { signal ->
                        logger.info(
                            "Retrying writing event on CDC queue due to: ${signal.failure().message}"
                        )
                    }
            )
    }
}
