package it.pagopa.wallet.services

import com.azure.core.http.rest.Response
import com.azure.storage.queue.models.SendMessageResult
import it.pagopa.wallet.client.WalletQueueClient
import it.pagopa.wallet.common.tracing.TracedMono
import it.pagopa.wallet.common.tracing.TracingUtilsTest
import it.pagopa.wallet.config.RetrySendPolicyConfig
import it.pagopa.wallet.config.properties.CdcQueueConfig
import java.time.Duration
import java.util.*
import org.bson.BsonDocument
import org.bson.BsonString
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import reactor.kotlin.test.test
import reactor.test.StepVerifier

class WalletPaymentCDCEventDispatcherServiceTest {

    private val config = CdcQueueConfig("", "", 100, 100)
    private val retrySendPolicyConfig: RetrySendPolicyConfig = RetrySendPolicyConfig(1, 100)
    private val walletQueueClient: WalletQueueClient = mock()
    private val tracingUtils = TracingUtilsTest.getMock()
    private val loggingEventDispatcherService =
        WalletPaymentCDCEventDispatcherService(
            walletQueueClient,
            tracingUtils,
            config,
            retrySendPolicyConfig
        )

    @Test
    fun `should dispatch WalletCreatedEvent from WalletAdded domain event`() {
        val walletId = UUID.randomUUID().toString()
        val walletCreatedLoggingEvent =
            BsonDocument().apply { append("walletId", BsonString(walletId)) }

        given { walletQueueClient.sendWalletEvent(any(), any(), any()) }
            .willAnswer { Mono.just(mock() as Response<SendMessageResult>) }

        loggingEventDispatcherService
            .dispatchEvent(walletCreatedLoggingEvent)
            .test()
            .assertNext { Assertions.assertEquals(walletCreatedLoggingEvent, it) }
            .verifyComplete()

        argumentCaptor<BsonDocument> {
            verify(walletQueueClient, times(1))
                .sendWalletEvent(capture(), eq(Duration.ofSeconds(config.timeoutWalletCdc)), any())
            Assertions.assertEquals(
                walletCreatedLoggingEvent.getString("walletId"),
                lastValue.getString("walletId")
            )
            verify(tracingUtils, times(1)).traceMonoQueue(any(), any<TracedMono<Any>>())
        }
    }

    @Test
    fun `should dispatch WalletCreatedEvent from WalletAdded domain event on second retry`() {
        val walletId = UUID.randomUUID().toString()
        val walletCreatedLoggingEvent =
            BsonDocument().apply { append("walletId", BsonString(walletId)) }

        given { walletQueueClient.sendWalletEvent(any(), any(), any()) }
            .willAnswer {
                Mono.error<Response<SendMessageResult>>(RuntimeException("First attempt failed"))
            }
            .willAnswer { Mono.just(mock() as Response<SendMessageResult>) }

        StepVerifier.create(loggingEventDispatcherService.dispatchEvent(walletCreatedLoggingEvent))
            .expectSubscription()
            .expectNextCount(1)
            .verifyComplete()
    }
}
