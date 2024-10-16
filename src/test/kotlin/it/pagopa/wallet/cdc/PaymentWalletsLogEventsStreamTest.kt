package it.pagopa.wallet.cdc

import com.mongodb.client.model.changestream.ChangeStreamDocument
import it.pagopa.wallet.config.properties.ChangeStreamOptionsConfig
import it.pagopa.wallet.services.ResumePolicyService
import it.pagopa.wallet.services.WalletPaymentCDCEventDispatcherService
import it.pagopa.wallet.util.ChangeStreamDocumentUtil
import java.time.Instant
import org.bson.BsonDocument
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.mongodb.core.ChangeStreamEvent
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
                1
            )
    }

    @Test
    fun `change stream produces new Document`() {
        val expectedDocument =
            Document("walletId", "testWallet")
                .append("_class", "testEvent")
                .append("timestamp", "2024-09-20T09:16:43.705881111Z")
        val expectedChangeStreamDocument =
            ChangeStreamEvent(
                ChangeStreamDocument(
                    null,
                    BsonDocument(),
                    null,
                    null,
                    expectedDocument,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                ),
                BsonDocument::class.java,
                mongoConverter
            )
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

        doNothing().`when`(resumePolicyService).saveResumeTimestamp(anyOrNull())

        given { walletPaymentCDCEventDispatcherService.dispatchEvent(anyOrNull()) }
            .willReturn(Mono.just(expectedDocument))

        StepVerifier.create(paymentWalletsLogEventsStream.streamPaymentWalletsLogEvents())
            .expectNext(expectedDocument)
            .verifyComplete()
    }

    @Test
    fun `change stream throws error and continues to listen`() {
        val expectedChangeStreamDocument =
            ChangeStreamEvent(
                ChangeStreamDocument(
                    null,
                    BsonDocument(),
                    null,
                    null,
                    Document("walletId", "testWallet")
                        .append("_class", "testEvent")
                        .append("timestamp", "2024-09-20T09:16:43.705881111Z"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                ),
                BsonDocument::class.java,
                mongoConverter
            )
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

        given { resumePolicyService.getResumeTimestamp() }.willReturn(Instant.now())

        doNothing().`when`(resumePolicyService).saveResumeTimestamp(anyOrNull())

        given { walletPaymentCDCEventDispatcherService.dispatchEvent(anyOrNull()) }
            .willThrow(IllegalArgumentException::class)

        StepVerifier.create(paymentWalletsLogEventsStream.streamPaymentWalletsLogEvents())
            .verifyComplete()

        verify(walletPaymentCDCEventDispatcherService, times(3)).dispatchEvent(anyOrNull())
    }

    @Test
    fun `save token throws error and continues to listen`() {
        val expectedDocument =
            Document("walletId", "testWallet")
                .append("_class", "testEvent")
                .append("timestamp", "2024-09-20T09:16:43.705881111Z")
        val expectedChangeStreamDocument =
            ChangeStreamEvent(
                ChangeStreamDocument(
                    null,
                    BsonDocument(),
                    null,
                    null,
                    expectedDocument,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                ),
                BsonDocument::class.java,
                mongoConverter
            )
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

        given { resumePolicyService.getResumeTimestamp() }.willReturn(Instant.now())

        given { resumePolicyService.saveResumeTimestamp(anyOrNull()) }
            .willThrow(IllegalArgumentException::class)

        given { walletPaymentCDCEventDispatcherService.dispatchEvent(anyOrNull()) }
            .willReturn(Mono.just(expectedDocument))

        StepVerifier.create(paymentWalletsLogEventsStream.streamPaymentWalletsLogEvents())
            .verifyComplete()

        verify(walletPaymentCDCEventDispatcherService, times(3)).dispatchEvent(anyOrNull())
    }

    @Test
    fun `save token success and continues to listen`() {
        val expectedDocument =
            Document("walletId", "testWallet")
                .append("_class", "testEvent")
                .append("timestamp", "2024-09-20T09:16:43.705881111Z")
        val expectedChangeStreamDocument =
            ChangeStreamEvent(
                ChangeStreamDocument(
                    null,
                    BsonDocument(),
                    null,
                    null,
                    expectedDocument,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                ),
                BsonDocument::class.java,
                mongoConverter
            )
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

        given { resumePolicyService.getResumeTimestamp() }.willReturn(Instant.now())

        doNothing().`when`(resumePolicyService).saveResumeTimestamp(anyOrNull())

        given { walletPaymentCDCEventDispatcherService.dispatchEvent(anyOrNull()) }
            .willReturn(Mono.just(expectedDocument))

        StepVerifier.create(paymentWalletsLogEventsStream.streamPaymentWalletsLogEvents())
            .expectNextCount(3)
            .verifyComplete()

        verify(walletPaymentCDCEventDispatcherService, times(3)).dispatchEvent(anyOrNull())
        verify(resumePolicyService, times(3)).saveResumeTimestamp(anyOrNull())
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

        doNothing().`when`(resumePolicyService).saveResumeTimestamp(anyOrNull())

        given { walletPaymentCDCEventDispatcherService.dispatchEvent(anyOrNull()) }
            .willReturn(Mono.just(expectedDocumentNull))

        StepVerifier.create(paymentWalletsLogEventsStream.streamPaymentWalletsLogEvents())
            .expectNextCount(3)
            .verifyComplete()

        verify(walletPaymentCDCEventDispatcherService, times(3)).dispatchEvent(anyOrNull())
        verify(resumePolicyService, times(3)).saveResumeTimestamp(anyOrNull())
    }
}
