package it.pagopa.wallet.services

import it.pagopa.wallet.common.ExclusiveLockDocument
import it.pagopa.wallet.config.properties.RedisJobLockPolicyConfig
import it.pagopa.wallet.exceptions.LockNotAcquiredException
import java.time.Duration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CdcLockService(
    @Autowired
    private val exclusiveLockRedisTemplate: ReactiveRedisTemplate<String, ExclusiveLockDocument>,
    @Autowired private val redisJobLockPolicyConfig: RedisJobLockPolicyConfig
) {
    companion object {
        const val LOCK_HOLDER_NAME = "pagopa-payment-wallet-cdc-service"
    }

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    /** Emits true if the lock was acquired, false if another instance already holds it. */
    fun acquireEventLock(eventId: String): Mono<Boolean> {
        logger.debug("Trying to acquire lock for event: {}", eventId)
        val lockKey = redisJobLockPolicyConfig.getLockNameByEventId(eventId)
        return Mono.defer {
                exclusiveLockRedisTemplate
                    .opsForValue()
                    .setIfAbsent(
                        lockKey,
                        ExclusiveLockDocument(lockKey, LOCK_HOLDER_NAME),
                        Duration.ofMillis(redisJobLockPolicyConfig.ttlInMs)
                    )
            }
            .onErrorMap { LockNotAcquiredException(eventId, it) }
    }
}
