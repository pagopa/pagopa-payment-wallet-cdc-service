package it.pagopa.wallet.services

import it.pagopa.wallet.config.properties.RedisJobLockPolicyConfig
import it.pagopa.wallet.exceptions.LockNotAcquiredException
import java.util.concurrent.TimeUnit
import org.redisson.api.RedissonReactiveClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CdcLockService(
    @Autowired private val redissonClient: RedissonReactiveClient,
    @Autowired private val redisJobLockPolicyConfig: RedisJobLockPolicyConfig
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun acquireEventLock(eventId: String): Mono<Unit> {
        logger.debug("Trying to acquire lock for event: {}", eventId)
        return redissonClient
            .getLock(redisJobLockPolicyConfig.getLockNameByEventId(eventId))
            .tryLock(
                redisJobLockPolicyConfig.waitTimeInMs,
                redisJobLockPolicyConfig.ttlInMs,
                TimeUnit.MILLISECONDS
            )
            .filter { it == true } // only lock acquired
            .doOnNext { logger.debug("Lock acquired for event: {}", eventId) }
            .onErrorMap {
                logger.error("Lock acquiring error for event: {}", eventId, it)
                LockNotAcquiredException(eventId, it)
            }
            .switchIfEmpty(Mono.error(LockNotAcquiredException(eventId)))
            .thenReturn(Unit)
    }
}
