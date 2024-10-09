package it.pagopa.wallet.cdc

import com.mongodb.client.model.changestream.ChangeStreamDocument
import it.pagopa.wallet.config.ChangeStreamOptionsConfig
import it.pagopa.wallet.config.RetrySendPolicyConfig
import it.pagopa.wallet.services.ResumePolicyService
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
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class PaymentWalletsLogEventsStreamTest {
    private val reactiveMongoTemplate: ReactiveMongoTemplate = mock()
    private val resumePolicyService: ResumePolicyService = mock()
    private val retrySendPolicyConfig: RetrySendPolicyConfig = RetrySendPolicyConfig(1, 100)
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
                retrySendPolicyConfig,
                resumePolicyService,
                1
            )
    }

    @Test
    fun `change stream produces new Document`() {
        val expectedDocument =
            ChangeStreamEvent(
                ChangeStreamDocument(
                    null,
                    BsonDocument(),
                    null,
                    null,
                    Document("walletId", "testWallet").append("_class", "testEvent"),
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
        val bsonDocumentFlux = Flux.just(expectedDocument)

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

        StepVerifier.create(paymentWalletsLogEventsStream.streamPaymentWalletsLogEvents())
            .expectNext(expectedDocument)
            .verifyComplete()
    }

    @Test
    fun `change stream throws error and continues to listen`() {
        val expectedMockDocument: ChangeStreamEvent<BsonDocument> = mock()
        val expectedDocument =
            ChangeStreamEvent(
                ChangeStreamDocument(
                    null,
                    BsonDocument(),
                    null,
                    null,
                    Document("walletId", "testWallet").append("_class", "testEvent"),
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
        val bsonDocumentFlux = Flux.just(expectedDocument, expectedMockDocument, expectedDocument)

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

        given { expectedMockDocument.raw }.willThrow(IllegalArgumentException())

        StepVerifier.create(paymentWalletsLogEventsStream.streamPaymentWalletsLogEvents())
            .recordWith { ArrayList() }
            .thenConsumeWhile { it.raw?.fullDocument != null }
            .expectRecordedMatches { it.size == 2 }
            .verifyComplete()
    }
}
