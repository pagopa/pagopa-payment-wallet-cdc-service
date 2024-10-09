package it.pagopa.wallet.services

import com.azure.core.http.rest.Response
import com.azure.storage.queue.models.SendMessageResult
import it.pagopa.wallet.client.WalletQueueClient
import it.pagopa.wallet.common.tracing.TracedMono
import it.pagopa.wallet.common.tracing.TracingUtilsTest
import it.pagopa.wallet.config.RetrySendPolicyConfig
import it.pagopa.wallet.config.properties.CdcQueueConfig
import it.pagopa.wallet.util.AzureQueueTestUtils
import java.time.Duration
import java.util.*
import org.bson.BsonDocument
import org.bson.BsonString
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import reactor.kotlin.test.test

class WalletPaymentCDCEventDispatcherServiceTest {

    private val config = CdcQueueConfig("", "", 100, 100)

    private val walletQueueClient: WalletQueueClient = mock()
    private val tracingUtils = TracingUtilsTest.getMock()
    private val retrySendPolicyConfig: RetrySendPolicyConfig = RetrySendPolicyConfig(1, 100)
    private val loggingEventDispatcherService =
        WalletPaymentCDCEventDispatcherService(
            walletQueueClient,
            tracingUtils,
            config,
            retrySendPolicyConfig
        )

    @BeforeEach
    fun setup() {
        given { walletQueueClient.sendWalletEvent(any(), any(), any(), any()) }
            .willAnswer { AzureQueueTestUtils.QUEUE_SUCCESSFUL_RESPONSE }
    }

    @Test
    fun `should dispatch WalletCreatedEvent from WalletAdded domain event 2`() {
        val walletId = UUID.randomUUID().toString()
        val walletCreatedLoggingEvent =
            BsonDocument().apply { append("walletId", BsonString(walletId)) }

        loggingEventDispatcherService
            .dispatchEvent(walletCreatedLoggingEvent)
            .test()
            .assertNext { Assertions.assertEquals(walletCreatedLoggingEvent, it) }
            .verifyComplete()

        argumentCaptor<BsonDocument> {
            verify(walletQueueClient, times(1))
                .sendWalletEvent(
                    capture(),
                    eq(Duration.ofSeconds(config.timeoutWalletExpired)),
                    any(),
                    any()
                )
            Assertions.assertEquals(
                walletCreatedLoggingEvent.getString("walletId"),
                lastValue.getString("walletId")
            )
            verify(tracingUtils, times(1)).traceMonoQueue(any(), any<TracedMono<Any>>())
        }
    }

    @Test
    fun `should return error if queue dispatching fails`() {
        val walletId = UUID.randomUUID().toString()
        val walletCreatedLoggingEvent =
            BsonDocument().apply { append("walletId", BsonString(walletId)) }
        given { walletQueueClient.sendWalletEvent(any(), any(), any(), any()) }
            .willAnswer {
                Mono.error<Response<SendMessageResult>>(RuntimeException("Fail to publish message"))
            }

        loggingEventDispatcherService.dispatchEvent(walletCreatedLoggingEvent).test().expectError()
        verify(tracingUtils, times(1)).traceMonoQueue(any(), any<TracedMono<Any>>())
    }
}
