package it.pagopa.wallet.cdc

import it.pagopa.wallet.config.ChangeStreamOptionsConfig
import it.pagopa.wallet.config.RetrySendPolicyConfig
import it.pagopa.wallet.services.ResumePolicyService
import java.time.Duration
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
import reactor.util.retry.Retry

@Component
class PaymentWalletsLogEventsStream(
    @Autowired private val reactiveMongoTemplate: ReactiveMongoTemplate,
    @Autowired private val changeStreamOptionsConfig: ChangeStreamOptionsConfig,
    @Autowired private val retrySendPolicyConfig: RetrySendPolicyConfig,
    @Autowired private val redisResumePolicyService: ResumePolicyService,
    @Value("\${cdc.resume.saveInterval}") private val saveInterval: Int
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
                        .resumeAt(redisResumePolicyService.getResumeTimestamp())
                        .build(),
                    BsonDocument::class.java
                )
                // Process the elements of the Flux
                .flatMap {
                    Mono.defer {
                            logger.info(
                                "Handling new change stream event of type {} for wallet with id {} created on {}",
                                it.raw?.fullDocument?.get("_class"),
                                it.raw?.fullDocument?.get("walletId"),
                                it.raw?.fullDocument?.get("timestamp")
                            )
                            Mono.just(it)
                        }
                        .retryWhen(
                            Retry.fixedDelay(
                                    retrySendPolicyConfig.maxAttempts,
                                    Duration.ofMillis(retrySendPolicyConfig.intervalInMs)
                                )
                                .filter { t -> t is Exception }
                        )
                        .onErrorResume {
                            logger.error("Error during event handling : ", it)
                            Mono.empty<ChangeStreamEvent<BsonDocument>>()
                        }
                }
                // Save resume token every n emitted elements
                .index()
                .flatMap {
                    Mono.defer {
                            if (it.t1.absoluteValue.plus(1).mod(saveInterval) == 0) {
                                redisResumePolicyService.saveResumeTimestamp(Instant.now())
                            }
                            Mono.just(it.t2)
                        }
                        .onErrorResume {
                            logger.error("Error saving resume policy: ", it)
                            Mono.empty<ChangeStreamEvent<BsonDocument>>()
                        }
                }
                .doOnError { logger.info("Error listening to change stream: ", it) }

        return flux
    }
}
