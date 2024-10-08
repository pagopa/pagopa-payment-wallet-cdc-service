package it.pagopa.wallet.client

import com.azure.core.http.rest.Response
import com.azure.core.util.BinaryData
import com.azure.core.util.serializer.JsonSerializer
import com.azure.storage.queue.QueueAsyncClient
import com.azure.storage.queue.models.SendMessageResult
import it.pagopa.wallet.common.QueueEvent
import it.pagopa.wallet.common.tracing.QueueTracingInfo
import java.time.Duration
import org.bson.BsonDocument
import reactor.core.publisher.Mono

class WalletQueueClient(
    private val expirationCdcQueueClient: QueueAsyncClient,
    private val jsonSerializer: JsonSerializer,
    private val ttl: Duration
) {

    fun sendWalletCreatedEvent(
        event: BsonDocument,
        delay: Duration,
        tracingInfo: QueueTracingInfo
    ): Mono<Response<SendMessageResult>> {
        val queueEvent = QueueEvent(event, tracingInfo)
        return BinaryData.fromObjectAsync(queueEvent, jsonSerializer).flatMap {
            expirationCdcQueueClient.sendMessageWithResponse(it, delay, ttl)
        }
    }
}
