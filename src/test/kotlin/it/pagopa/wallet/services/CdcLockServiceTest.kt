package it.pagopa.wallet.services

import it.pagopa.wallet.common.ExclusiveLockDocument
import it.pagopa.wallet.config.properties.RedisJobLockPolicyConfig
import it.pagopa.wallet.exceptions.LockNotAcquiredException
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mockito.kotlin.*
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import reactor.core.publisher.Mono
import reactor.kotlin.test.test

class CdcLockServiceTest {
    private val valueOperations: ReactiveValueOperations<String, ExclusiveLockDocument> = mock()
    private val exclusiveLockRedisTemplate: ReactiveRedisTemplate<String, ExclusiveLockDocument> =
        mock()
    private val redisJobLockPolicyConfig: RedisJobLockPolicyConfig =
        RedisJobLockPolicyConfig("lockkeyspace", 20, 2)
    private val cdcLockService: CdcLockService =
        CdcLockService(exclusiveLockRedisTemplate, redisJobLockPolicyConfig)

    private val eventId = "eventId"
    private val expectedLockKey = "lockkeyspace:lock:$eventId"

    /*+ Lock tests **/

    @Test
    fun `Should acquire lock`() {
        // pre-requisites
        given(exclusiveLockRedisTemplate.opsForValue()).willReturn(valueOperations)
        given(valueOperations.setIfAbsent(any(), any(), any<Duration>()))
            .willReturn(Mono.just(true))

        // Test
        cdcLockService.acquireEventLock(eventId).test().expectNext(true).verifyComplete()

        // verifications
        verifyLockRequested()
    }

    @Test
    fun `Should not acquire lock already held by another instance`() {
        // pre-requisites
        given(exclusiveLockRedisTemplate.opsForValue()).willReturn(valueOperations)
        given(valueOperations.setIfAbsent(any(), any(), any<Duration>()))
            .willReturn(Mono.just(false))

        // Test
        cdcLockService.acquireEventLock(eventId).test().expectNext(false).verifyComplete()

        // verifications
        verifyLockRequested()
    }

    @Test
    fun `Should throw LockNotAcquiredException when setIfAbsent emits error`() {
        // pre-requisites
        given(exclusiveLockRedisTemplate.opsForValue()).willReturn(valueOperations)
        given(valueOperations.setIfAbsent(any(), any(), any<Duration>()))
            .willReturn(Mono.error(RuntimeException("Test exception")))

        // Test
        cdcLockService
            .acquireEventLock(eventId)
            .test()
            .expectError(LockNotAcquiredException::class.java)
            .verify()

        // verifications
        verifyLockRequested()
    }

    @Test
    fun `Should throw LockNotAcquiredException when opsForValue throw exception`() {
        // pre-requisites
        given(exclusiveLockRedisTemplate.opsForValue())
            .willThrow(RuntimeException("Test exception"))

        // Test
        cdcLockService
            .acquireEventLock(eventId)
            .test()
            .expectError(LockNotAcquiredException::class.java)
            .verify()

        // verifications
        verify(exclusiveLockRedisTemplate, times(1)).opsForValue()
        verifyNoInteractions(valueOperations)
    }

    private fun verifyLockRequested() {
        verify(valueOperations, times(1))
            .setIfAbsent(
                eq(expectedLockKey),
                argThat {
                    assertEquals(expectedLockKey, this.id)
                    assertEquals(CdcLockService.LOCK_HOLDER_NAME, this.holderName)
                    true
                },
                eq(Duration.ofMillis(20))
            )
    }
}
