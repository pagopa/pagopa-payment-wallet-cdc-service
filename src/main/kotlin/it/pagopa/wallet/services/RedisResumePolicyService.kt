package it.pagopa.wallet.services

import it.pagopa.wallet.config.properties.RedisResumePolicyConfig
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class RedisResumePolicyService(
    @Autowired private val redisTemplate: TimestampRedisTemplate,
    @Autowired private val redisResumePolicyConfig: RedisResumePolicyConfig
) : ResumePolicyService {
    private val logger = LoggerFactory.getLogger(RedisResumePolicyService::class.java)

    override fun getResumeTimestamp(): Mono<Instant> {
        return redisTemplate
            .findByKeyspaceAndTarget(
                redisResumePolicyConfig.keyspace,
                redisResumePolicyConfig.target
            )
            .switchIfEmpty {
                logger.warn(
                    "Resume timestamp not found on Redis, fallback on Instant.now()-{} minutes",
                    redisResumePolicyConfig.fallbackInMin
                )
                Mono.just(
                    Instant.now().minus(redisResumePolicyConfig.fallbackInMin, ChronoUnit.MINUTES)
                )
            }
    }

    override fun saveResumeTimestamp(timestamp: Instant): Mono<Boolean> {
        logger.debug("Saving instant: {}", timestamp.toString())
        return redisTemplate.save(
            redisResumePolicyConfig.keyspace,
            redisResumePolicyConfig.target,
            timestamp,
            Duration.ofMinutes(redisResumePolicyConfig.ttlInMin)
        )
    }
}
