package it.pagopa.wallet.services

import java.time.Duration
import java.time.Instant
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class TimestampRedisTemplate(
    @Autowired private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Instant>
) {

    fun save(keyspace: String, cdcTarget: String, instant: Instant, ttl: Duration): Mono<Boolean> {
        val key = compoundKeyWithKeyspace(keyspace, cdcTarget)
        return reactiveRedisTemplate.opsForValue().set(key, instant, ttl)
    }

    fun findByKeyspaceAndTarget(keyspace: String, cdcTarget: String): Mono<Instant> {
        val key = compoundKeyWithKeyspace(keyspace, cdcTarget)
        return reactiveRedisTemplate.opsForValue()[key]
    }

    private fun compoundKeyWithKeyspace(keyspace: String, cdcTarget: String): String {
        return "%s:%s:%s".format(keyspace, "time", cdcTarget)
    }
}
