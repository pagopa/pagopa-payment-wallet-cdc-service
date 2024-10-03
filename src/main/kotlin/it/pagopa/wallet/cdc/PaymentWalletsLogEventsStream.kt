package it.pagopa.wallet.cdc

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

@Component
class PaymentWalletsLogEventsStream(
    @Autowired private val reactiveMongoTemplate: ReactiveMongoTemplate
) : ApplicationListener<ApplicationReadyEvent> {
    private val logger = LoggerFactory.getLogger(PaymentWalletsLogEventsStream::class.java)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        this.streamPaymentWalletsLogEvents()
    }

    fun streamPaymentWalletsLogEvents() {
        val flux: Flux<ChangeStreamEvent<BsonDocument>> =
            reactiveMongoTemplate
                .changeStream(
                    "payment-wallets-log-events",
                    ChangeStreamOptions.builder()
                        .filter(
                            Aggregation.newAggregation(
                                Aggregation.match(
                                    Criteria.where("operationType")
                                        .`in`("insert", "update", "replace")
                                ),
                                Aggregation.project("fullDocument")
                            )
                        )
                        .build(),
                    BsonDocument::class.java
                )
                .flatMap<ChangeStreamEvent<BsonDocument>?> {
                    logger.info("Handling new change stream event: {}", it.raw?.fullDocument?.toJson())
                    Flux.just(it)
                }
                .onErrorContinue { throwable, obj ->
                    logger.error("Error for object {} : ", obj, throwable)
                }

        flux.subscribe()
    }
}
