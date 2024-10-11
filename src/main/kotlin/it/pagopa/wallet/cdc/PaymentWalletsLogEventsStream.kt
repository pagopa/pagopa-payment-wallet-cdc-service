package it.pagopa.wallet.cdc

import it.pagopa.wallet.config.properties.ChangeStreamOptionsConfig
import it.pagopa.wallet.services.ResumePolicyService
import it.pagopa.wallet.services.WalletPaymentCDCEventDispatcherService
import java.time.Instant
import kotlin.math.absoluteValue
import org.bson.BsonDocument
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
import reactor.util.function.Tuple2

@Component
class PaymentWalletsLogEventsStream(
    @Autowired private val reactiveMongoTemplate: ReactiveMongoTemplate,
    @Autowired private val changeStreamOptionsConfig: ChangeStreamOptionsConfig,
    @Autowired
    private val walletPaymentCDCEventDispatcherService: WalletPaymentCDCEventDispatcherService,
    @Autowired private val redisResumePolicyService: ResumePolicyService,
    @Value("\${cdc.resume.saveInterval}") private val saveInterval: Int
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
                        .resumeAt(redisResumePolicyService.getResumeTimestamp())
                        .build(),
                    BsonDocument::class.java
                )
                // Process the elements of the Flux
                .flatMap { processEvent(it) }
                // Save resume token every n emitted elements
                .index()
                .flatMap { saveCdcResumeToken(it) }
                .doOnError { logger.error("Error listening to change stream: ", it) }

        return flux
    }

    private fun processEvent(event: ChangeStreamEvent<BsonDocument>): Mono<BsonDocument> {
        return Mono.defer {
                walletPaymentCDCEventDispatcherService.dispatchEvent(
                    event.raw?.fullDocument?.toBsonDocument()
                )
            }
            .onErrorResume {
                logger.error("Error during event handling : ", it)
                Mono.empty()
            }
    }

    private fun saveCdcResumeToken(changeEventFluxIndex: Tuple2<Long, BsonDocument>): Mono<BsonDocument> {
        return Mono.defer {
                if (changeEventFluxIndex.t1.absoluteValue.plus(1).mod(saveInterval) == 0) {
                    val documentTimestamp = changeEventFluxIndex.t2["timestamp"]?.asString()?.value
                    val resumeTimestamp =
                        if (documentTimestamp != null) Instant.parse(documentTimestamp)
                        else Instant.now()

                    redisResumePolicyService.saveResumeTimestamp(resumeTimestamp)
                }
                Mono.just(changeEventFluxIndex.t2)
            }
            .onErrorResume {
                logger.error("Error saving resume policy: ", it)
                Mono.empty()
            }
    }
}
