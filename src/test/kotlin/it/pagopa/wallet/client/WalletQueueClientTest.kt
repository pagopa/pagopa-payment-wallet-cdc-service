import com.azure.core.http.rest.Response
import com.azure.core.util.BinaryData
import com.azure.core.util.serializer.JsonSerializer
import com.azure.storage.queue.QueueAsyncClient
import com.azure.storage.queue.models.SendMessageResult
import it.pagopa.wallet.client.WalletQueueClient
import it.pagopa.wallet.common.QueueEvent
import it.pagopa.wallet.common.tracing.QueueTracingInfo
import java.time.Duration
import org.bson.BsonDocument
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class WalletQueueClientTest {

    private val event = BsonDocument()
    private val tracingInfo = QueueTracingInfo("", "", "")
    private val ttl = Duration.ofMinutes(5)
    private val delay = Duration.ofSeconds(10)
    private val queueEvent = QueueEvent(event, tracingInfo)
    private val binaryData = BinaryData.fromObject(queueEvent)

    private val jsonSerializer: JsonSerializer = mock()
    private var cdcQueueClient: QueueAsyncClient = mock()
    private val walletQueueClient = WalletQueueClient(cdcQueueClient, jsonSerializer, ttl)

    @Test
    fun `sendWalletEvent should succeed without retry`() {

        given { jsonSerializer.serializeToBytes(queueEvent) }.willAnswer { binaryData.toBytes() }
        given { cdcQueueClient.sendMessageWithResponse(any<BinaryData>(), eq(delay), eq(ttl)) }
            .willAnswer { Mono.just(mock() as Response<SendMessageResult>) }

        StepVerifier.create(walletQueueClient.sendWalletEvent(event, delay, tracingInfo))
            .expectSubscription()
            .expectNextCount(1)
            .verifyComplete()
    }

    @Test
    fun `sendWalletEvent should succeed on second retry`() {

        given { jsonSerializer.serializeToBytes(queueEvent) }.willAnswer { binaryData.toBytes() }

        // First attempt fails
        given { cdcQueueClient.sendMessageWithResponse(any<BinaryData>(), eq(delay), eq(ttl)) }
            .willAnswer {
                Mono.error<Response<SendMessageResult>>(RuntimeException("First attempt failed"))
            }
            .willAnswer {
                Mono.just(mock() as Response<SendMessageResult>)
            } // Second attempt succeeds

        StepVerifier.create(walletQueueClient.sendWalletEvent(event, delay, tracingInfo))
            .expectSubscription()
            .expectNextCount(1)
            .verifyComplete()
    }
}
