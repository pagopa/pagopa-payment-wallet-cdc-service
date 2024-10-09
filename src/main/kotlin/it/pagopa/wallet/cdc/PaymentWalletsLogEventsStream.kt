package it.pagopa.wallet.cdc

import it.pagopa.wallet.config.ChangeStreamOptionsConfig
import it.pagopa.wallet.services.WalletPaymentCDCEventDispatcherService
import java.time.Instant
import org.bson.BsonDocument
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.data.mongodb.core.ChangeStreamOptions
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class PaymentWalletsLogEventsStream(
    @Autowired private val reactiveMongoTemplate: ReactiveMongoTemplate,
    @Autowired private val changeStreamOptionsConfig: ChangeStreamOptionsConfig,
    @Autowired
    private val walletPaymentCDCEventDispatcherService: WalletPaymentCDCEventDispatcherService
) : ApplicationListener<ApplicationReadyEvent> {
    private val logger = LoggerFactory.getLogger(PaymentWalletsLogEventsStream::class.java)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        this.streamPaymentWalletsLogEvents().subscribe()
    }

    fun streamPaymentWalletsLogEvents(): Flux<BsonDocument> {
        val flux: Flux<BsonDocument> =
            reactiveMongoTemplate
                .changeStream(
                    changeStreamOptionsConfig.collection,
                    ChangeStreamOptions.builder()
                        .filter(
                            Aggregation.newAggregation(
                                Aggregation.match(
                                    Criteria.where("operationType")
                                        .`in`(changeStreamOptionsConfig.operationType)
                                ),
                                Aggregation.project(changeStreamOptionsConfig.project)
                            )
                        )
                        .resumeAt(Instant.now())
                        .build(),
                    BsonDocument::class.java
                )
                .flatMap {
                    Mono.defer {
                            walletPaymentCDCEventDispatcherService.dispatchEvent(
                                it.raw?.fullDocument?.toBsonDocument()
                            )
                        }
                        .onErrorResume {
                            logger.error("Error during event handling : ", it)
                            Mono.empty<BsonDocument>()
                        }
                }

        return flux
    }
}
