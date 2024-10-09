package it.pagopa.wallet.client

import com.azure.core.http.rest.Response
import com.azure.core.util.BinaryData
import com.azure.core.util.serializer.JsonSerializer
import com.azure.storage.queue.QueueAsyncClient
import com.azure.storage.queue.models.SendMessageResult
import it.pagopa.wallet.cdc.PaymentWalletsLogEventsStream
import it.pagopa.wallet.common.QueueEvent
import it.pagopa.wallet.common.tracing.QueueTracingInfo
import java.time.Duration
import java.time.temporal.ChronoUnit
import org.bson.BsonDocument
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

class WalletQueueClient(
    private val cdcQueueClient: QueueAsyncClient,
    private val jsonSerializer: JsonSerializer,
    private val ttl: Duration
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
                Retry.backoff(3, Duration.of(1, ChronoUnit.SECONDS)).doBeforeRetry { signal ->
                    logger.info("Retrying due to: ${signal.failure().message}")
                }
            )
    }
}
