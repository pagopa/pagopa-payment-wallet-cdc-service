package it.pagopa.wallet.cdc

import com.mongodb.MongoQueryException
import com.mongodb.ServerAddress
import it.pagopa.wallet.config.properties.ChangeStreamOptionsConfig
import it.pagopa.wallet.config.properties.RetryStreamPolicyConfig
import it.pagopa.wallet.exceptions.LockNotAcquiredException
import it.pagopa.wallet.services.CdcLockService
import it.pagopa.wallet.services.ResumePolicyService
import it.pagopa.wallet.services.WalletPaymentCDCEventDispatcherService
import it.pagopa.wallet.util.ChangeStreamDocumentUtil
import java.time.Instant
import org.bson.BsonDocument
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.internal.verification.Times
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.convert.MongoConverter
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class PaymentWalletsLogEventsStreamTest {
    private val reactiveMongoTemplate: ReactiveMongoTemplate = mock()
    private val cdcLockService: CdcLockService = mock()
    private val retryStreamPolicyConfig: RetryStreamPolicyConfig = RetryStreamPolicyConfig(2, 100)
    private val walletPaymentCDCEventDispatcherService: WalletPaymentCDCEventDispatcherService =
        mock()
    private val resumePolicyService: ResumePolicyService = mock()
    private val changeStreamOptionsConfig: ChangeStreamOptionsConfig =
        ChangeStreamOptionsConfig("collection", ArrayList(), "project")
    private val mongoConverter: MongoConverter = mock()
    private lateinit var paymentWalletsLogEventsStream: PaymentWalletsLogEventsStream

    @BeforeEach
    fun initEventStream() {
        paymentWalletsLogEventsStream =
            PaymentWalletsLogEventsStream(
                reactiveMongoTemplate,
                changeStreamOptionsConfig,
                walletPaymentCDCEventDispatcherService,
                resumePolicyService,
                cdcLockService,
                retryStreamPolicyConfig,
                1
            )
    }

    @Test
    fun `change stream produces new Document`() {
        val expectedDocument =
            ChangeStreamDocumentUtil.getDocument(
                "testWallet",
                "testEvent",
                "2024-09-20T09:16:43.705881111Z"
            )
        val expectedChangeStreamDocument =
            ChangeStreamDocumentUtil.getChangeStreamEvent(expectedDocument, mongoConverter)
        val bsonDocumentFlux = Flux.just(expectedChangeStreamDocument)

        given {
                reactiveMongoTemplate.changeStream(
                    anyOrNull(),
                    anyOrNull(),
                    eq(BsonDocument::class.java)
                )
            }
            .willReturn(bsonDocumentFlux)

        given { resumePolicyService.getResumeTimestamp() }.willReturn(Instant.now())

        given { cdcLockService.acquireEventLock(any()) }.willReturn(Mono.just(Unit))

        doNothing().`when`(resumePolicyService).saveResumeTimestamp(anyOrNull())

        given { walletPaymentCDCEventDispatcherService.dispatchEvent(anyOrNull()) }
            .willReturn(Mono.just(expectedDocument))

        StepVerifier.create(paymentWalletsLogEventsStream.streamPaymentWalletsLogEvents())
            .expectNext(expectedDocument)
            .verifyComplete()

        verify(cdcLockService, Times(1)).acquireEventLock(expectedDocument.getString("_id"))
    }

    @Test
    fun `change stream throws error and continues to listen`() {
        val expectedDocument =
            ChangeStreamDocumentUtil.getDocument(
                "testWallet",
                "testEvent",
                "2024-09-20T09:16:43.705881111Z"
            )
        val expectedChangeStreamDocument =
            ChangeStreamDocumentUtil.getChangeStreamEvent(expectedDocument, mongoConverter)
        val bsonDocumentFlux =
            Flux.just(
                expectedChangeStreamDocument,
                expectedChangeStreamDocument,
                expectedChangeStreamDocument
            )

        given {
                reactiveMongoTemplate.changeStream(
                    anyOrNull(),
                    anyOrNull(),
                    eq(BsonDocument::class.java)
                )
            }
            .willReturn(bsonDocumentFlux)

        given { cdcLockService.acquireEventLock(any()) }.willReturn(Mono.just(Unit))

        given { resumePolicyService.getResumeTimestamp() }.willReturn(Instant.now())

        doNothing().`when`(resumePolicyService).saveResumeTimestamp(anyOrNull())

        given { walletPaymentCDCEventDispatcherService.dispatchEvent(anyOrNull()) }
            .willThrow(IllegalArgumentException::class)

        StepVerifier.create(paymentWalletsLogEventsStream.streamPaymentWalletsLogEvents())
            .verifyComplete()

        verify(walletPaymentCDCEventDispatcherService, times(3)).dispatchEvent(anyOrNull())
        verify(cdcLockService, Times(3)).acquireEventLock(expectedDocument.getString("_id"))
    }

    @Test
    fun `save token throws error and continues to listen`() {
        val expectedDocument =
            ChangeStreamDocumentUtil.getDocument(
                "testWallet",
                "testEvent",
                "2024-09-20T09:16:43.705881111Z"
            )
        val expectedChangeStreamDocument =
            ChangeStreamDocumentUtil.getChangeStreamEvent(expectedDocument, mongoConverter)
        val bsonDocumentFlux =
            Flux.just(
                expectedChangeStreamDocument,
                expectedChangeStreamDocument,
                expectedChangeStreamDocument
            )

        given {
                reactiveMongoTemplate.changeStream(
                    anyOrNull(),
                    anyOrNull(),
                    eq(BsonDocument::class.java)
                )
            }
            .willReturn(bsonDocumentFlux)

        given { cdcLockService.acquireEventLock(any()) }.willReturn(Mono.just(Unit))

        given { resumePolicyService.getResumeTimestamp() }.willReturn(Instant.now())

        given { resumePolicyService.saveResumeTimestamp(anyOrNull()) }
            .willThrow(IllegalArgumentException::class)

        given { walletPaymentCDCEventDispatcherService.dispatchEvent(anyOrNull()) }
            .willReturn(Mono.just(expectedDocument))

        StepVerifier.create(paymentWalletsLogEventsStream.streamPaymentWalletsLogEvents())
            .verifyComplete()

        verify(walletPaymentCDCEventDispatcherService, times(3)).dispatchEvent(anyOrNull())
        verify(cdcLockService, Times(3)).acquireEventLock(expectedDocument.getString("_id"))
    }

    @Test
    fun `save token success and continues to listen`() {
        val expectedDocument =
            ChangeStreamDocumentUtil.getDocument(
                "testWallet",
                "testEvent",
                "2024-09-20T09:16:43.705881111Z"
            )
        val expectedChangeStreamDocument =
            ChangeStreamDocumentUtil.getChangeStreamEvent(expectedDocument, mongoConverter)
        val bsonDocumentFlux =
            Flux.just(
                expectedChangeStreamDocument,
                expectedChangeStreamDocument,
                expectedChangeStreamDocument
            )

        given {
                reactiveMongoTemplate.changeStream(
                    anyOrNull(),
                    anyOrNull(),
                    eq(BsonDocument::class.java)
                )
            }
            .willReturn(bsonDocumentFlux)

        given { cdcLockService.acquireEventLock(any()) }.willReturn(Mono.just(Unit))

        given { resumePolicyService.getResumeTimestamp() }.willReturn(Instant.now())

        doNothing().`when`(resumePolicyService).saveResumeTimestamp(anyOrNull())

        given { walletPaymentCDCEventDispatcherService.dispatchEvent(anyOrNull()) }
            .willReturn(Mono.just(expectedDocument))

        StepVerifier.create(paymentWalletsLogEventsStream.streamPaymentWalletsLogEvents())
            .expectNextCount(3)
            .verifyComplete()

        verify(walletPaymentCDCEventDispatcherService, times(3)).dispatchEvent(anyOrNull())
        verify(resumePolicyService, times(3)).saveResumeTimestamp(anyOrNull())
        verify(cdcLockService, Times(3)).acquireEventLock(expectedDocument.getString("_id"))
    }

    @Test
    fun `document with null, empty and blank timestamp and continues to listen`() {
        val expectedDocumentNull =
            ChangeStreamDocumentUtil.getDocument("testWallet", "testEvent", null)
        val expectedDocumentEmpty =
            ChangeStreamDocumentUtil.getDocument("testWallet", "testEvent", "")
        val expectedDocumentBlank =
            ChangeStreamDocumentUtil.getDocument("testWallet", "testEvent", "  ")
        val expectedChangeStreamDocumentNull =
            ChangeStreamDocumentUtil.getChangeStreamEvent(expectedDocumentNull, mongoConverter)
        val expectedChangeStreamDocumentEmpty =
            ChangeStreamDocumentUtil.getChangeStreamEvent(expectedDocumentEmpty, mongoConverter)
        val expectedChangeStreamDocumentBlank =
            ChangeStreamDocumentUtil.getChangeStreamEvent(expectedDocumentBlank, mongoConverter)
        val bsonDocumentFlux =
            Flux.just(
                expectedChangeStreamDocumentNull,
                expectedChangeStreamDocumentEmpty,
                expectedChangeStreamDocumentBlank
            )

        given {
                reactiveMongoTemplate.changeStream(
                    anyOrNull(),
                    anyOrNull(),
                    eq(BsonDocument::class.java)
                )
            }
            .willReturn(bsonDocumentFlux)

        given { resumePolicyService.getResumeTimestamp() }.willReturn(Instant.now())

        given { cdcLockService.acquireEventLock(any()) }.willReturn(Mono.just(Unit))

        doNothing().`when`(resumePolicyService).saveResumeTimestamp(anyOrNull())

        given { walletPaymentCDCEventDispatcherService.dispatchEvent(anyOrNull()) }
            .willReturn(Mono.just(expectedDocumentNull))
            .willReturn(Mono.just(expectedDocumentEmpty))
            .willReturn(Mono.just(expectedDocumentBlank))

        StepVerifier.create(paymentWalletsLogEventsStream.streamPaymentWalletsLogEvents())
            .expectNextCount(3)
            .verifyComplete()

        verify(walletPaymentCDCEventDispatcherService, times(3)).dispatchEvent(anyOrNull())
        verify(resumePolicyService, times(3)).saveResumeTimestamp(anyOrNull())
        verify(cdcLockService, Times(3)).acquireEventLock(any())
    }

    @Test
    fun `change stream disconnects and retries to listen`() {
        given {
                reactiveMongoTemplate.changeStream(
                    anyOrNull(),
                    anyOrNull(),
                    eq(BsonDocument::class.java)
                )
            }
            .willThrow(MongoQueryException(BsonDocument(), ServerAddress()))

        given { resumePolicyService.getResumeTimestamp() }.willReturn(Instant.now())

        given { cdcLockService.acquireEventLock(any()) }.willReturn(Mono.just(Unit))

        doNothing().`when`(resumePolicyService).saveResumeTimestamp(anyOrNull())

        given { walletPaymentCDCEventDispatcherService.dispatchEvent(anyOrNull()) }
            .willThrow(IllegalArgumentException::class)

        StepVerifier.create(paymentWalletsLogEventsStream.streamPaymentWalletsLogEvents())
            .verifyError()

        verify(reactiveMongoTemplate, times(3))
            .changeStream(anyOrNull(), anyOrNull(), eq(BsonDocument::class.java))
        verify(cdcLockService, Times(0)).acquireEventLock(any())
    }

    @Test
    fun `change stream does not acquire lock`() {
        val expectedDocument =
            ChangeStreamDocumentUtil.getDocument(
                "testWallet",
                "testEvent",
                "2024-09-20T09:16:43.705881111Z"
            )
        val expectedChangeStreamDocument =
            ChangeStreamDocumentUtil.getChangeStreamEvent(expectedDocument, mongoConverter)
        val bsonDocumentFlux = Flux.just(expectedChangeStreamDocument)

        given {
                reactiveMongoTemplate.changeStream(
                    anyOrNull(),
                    anyOrNull(),
                    eq(BsonDocument::class.java)
                )
            }
            .willReturn(bsonDocumentFlux)

        given { resumePolicyService.getResumeTimestamp() }.willReturn(Instant.now())

        given { cdcLockService.acquireEventLock(any()) }
            .willThrow(LockNotAcquiredException("Test error"))

        doNothing().`when`(resumePolicyService).saveResumeTimestamp(anyOrNull())

        given { walletPaymentCDCEventDispatcherService.dispatchEvent(anyOrNull()) }
            .willReturn(Mono.just(expectedDocument))

        StepVerifier.create(paymentWalletsLogEventsStream.streamPaymentWalletsLogEvents())
            .verifyComplete()

        verify(cdcLockService, Times(1)).acquireEventLock(expectedDocument.getString("_id"))
        verify(walletPaymentCDCEventDispatcherService, Times(0)).dispatchEvent(any())
        verify(resumePolicyService, Times(0)).saveResumeTimestamp(any())
    }
}
