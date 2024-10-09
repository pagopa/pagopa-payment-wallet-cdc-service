package it.pagopa.wallet.cdc

import it.pagopa.wallet.config.ChangeStreamOptionsConfig
import it.pagopa.wallet.services.WalletPaymentCDCEventDispatcherService
import java.time.Instant
import org.bson.BsonDocument
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.data.mongodb.core.ChangeStreamEvent
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

    fun streamPaymentWalletsLogEvents(): Flux<ChangeStreamEvent<BsonDocument>> {
        val flux: Flux<ChangeStreamEvent<BsonDocument>> =
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
                .flatMap { event ->
                    Mono.defer {
                            logger.info(
                                "Handling new change stream event of type {} for wallet with id {}",
                                event.raw?.fullDocument?.get("_class"),
                                event.raw?.fullDocument?.get("walletId")
                            )
                            walletPaymentCDCEventDispatcherService.dispatchEvent(
                                event.raw?.fullDocument?.toBsonDocument()
                            )
                        }
                        .thenReturn(event)
                        .onErrorResume { throwable ->
                            logger.error("Error during event handling: ", throwable)
                            Mono.error(throwable)
                        }
                }
                .onErrorResume {
                    logger.error("Error during event handling: ", it)
                    Mono.empty<ChangeStreamEvent<BsonDocument>>()
                }

        return flux
    }
}
