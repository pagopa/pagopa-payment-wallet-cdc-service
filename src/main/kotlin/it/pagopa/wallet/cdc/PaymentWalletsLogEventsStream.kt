package it.pagopa.wallet.cdc

import it.pagopa.wallet.config.properties.ChangeStreamOptionsConfig
import it.pagopa.wallet.services.ResumePolicyService
import it.pagopa.wallet.services.WalletPaymentCDCEventDispatcherService
import java.time.Instant
import org.bson.BsonDocument
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
    private val walletPaymentCDCEventDispatcherService: WalletPaymentCDCEventDispatcherService,
    @Autowired private val redisResumePolicyService: ResumePolicyService,
    @Value("\${cdc.resume.saveInterval}") private val saveInterval: Int
) : ApplicationListener<ApplicationReadyEvent> {
    private val logger = LoggerFactory.getLogger(PaymentWalletsLogEventsStream::class.java)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        this.streamPaymentWalletsLogEvents().subscribe()
    }

    fun streamPaymentWalletsLogEvents(): Flux<Document> {
        val flux: Flux<Document> =
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
                .flatMap { processEvent(it.raw?.fullDocument) }
                // Save resume token every n emitted elements
                .index { changeEventFluxIndex, changeEventDocument ->
                    Pair(changeEventFluxIndex, changeEventDocument)
                }
                .flatMap { (changeEventFluxIndex, changeEventDocument) ->
                    saveCdcResumeToken(changeEventFluxIndex, changeEventDocument)
                }
                .doOnError { logger.error("Error listening to change stream: ", it) }

        return flux
    }

    private fun processEvent(event: Document?): Mono<Document> {
        return Mono.defer { walletPaymentCDCEventDispatcherService.dispatchEvent(event) }
            .onErrorResume {
                logger.error("Error during event handling : ", it)
                Mono.empty()
            }
    }

    private fun saveCdcResumeToken(
        changeEventFluxIndex: Long,
        changeEventDocument: Document
    ): Mono<Document> {
        return Mono.defer {
                if (changeEventFluxIndex.plus(1).mod(saveInterval) == 0) {
                    val documentTimestamp = changeEventDocument.getString("timestamp")
                    val resumeTimestamp =
                        if (!documentTimestamp.isNullOrBlank()) Instant.parse(documentTimestamp)
                        else Instant.now()

                    redisResumePolicyService.saveResumeTimestamp(resumeTimestamp)
                }
                Mono.just(changeEventDocument)
            }
            .onErrorResume {
                logger.error("Error saving resume policy: ", it)
                Mono.empty()
            }
    }
}
