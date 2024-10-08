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
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
class WalletQueueClientTest {

    @Mock private lateinit var cdcQueueClient: QueueAsyncClient

    @Mock private lateinit var jsonSerializer: JsonSerializer

    @InjectMocks private lateinit var walletQueueClient: WalletQueueClient

    @Captor private lateinit var binaryDataCaptor: ArgumentCaptor<BinaryData>

    @Test
    fun `sendWalletEvent should retry on failure and succeed`() {
        val event = BsonDocument()
        val tracingInfo = QueueTracingInfo("", "", "")
        val ttl = Duration.ofMinutes(5)
        val delay = Duration.ofSeconds(10)
        val queueEvent = QueueEvent(event, tracingInfo)
        val binaryData = BinaryData.fromObject(queueEvent)

        whenever(jsonSerializer.serializeToBytes(queueEvent)).thenReturn(binaryData.toBytes())
        whenever(cdcQueueClient.sendMessageWithResponse(any<BinaryData>(), eq(delay), eq(ttl)))
            .thenReturn(Mono.error(RuntimeException("Temporary error")))
            .thenReturn(Mono.just(mock() as Response<SendMessageResult>))

        StepVerifier.create(walletQueueClient.sendWalletEvent(event, delay, tracingInfo))
            .expectSubscription()
            .expectError(RuntimeException::class.java)
            .verify()

        verify(cdcQueueClient, times(2))
            .sendMessageWithResponse(any<BinaryData>(), eq(delay), eq(ttl))
    }

    @Test
    fun `sendWalletEvent should succeed without retry`() {
        val event = BsonDocument()
        val tracingInfo = QueueTracingInfo("", "", "")
        val ttl = Duration.ofMinutes(5)
        val delay = Duration.ofSeconds(10)
        val queueEvent = QueueEvent(event, tracingInfo)
        val binaryData = BinaryData.fromObject(queueEvent)

        whenever(jsonSerializer.serializeToBytes(queueEvent)).thenReturn(binaryData.toBytes())
        whenever(cdcQueueClient.sendMessageWithResponse(any<BinaryData>(), eq(delay), eq(ttl)))
            .thenReturn(Mono.just(mock() as Response<SendMessageResult>))

        StepVerifier.create(walletQueueClient.sendWalletEvent(event, delay, tracingInfo))
            .expectSubscription()
            .expectNextCount(1)
            .verifyComplete()

        verify(cdcQueueClient, times(1))
            .sendMessageWithResponse(any<BinaryData>(), eq(delay), eq(ttl))
    }
}
