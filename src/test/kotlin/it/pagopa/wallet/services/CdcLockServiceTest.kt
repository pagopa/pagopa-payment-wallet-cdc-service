package it.pagopa.wallet.services

import it.pagopa.wallet.config.properties.RedisJobLockPolicyConfig
import it.pagopa.wallet.exceptions.LockNotAcquiredException
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlinx.coroutines.reactor.mono
import org.mockito.kotlin.*
import org.redisson.api.RLockReactive
import org.redisson.api.RedissonReactiveClient
import reactor.core.publisher.Mono
import reactor.kotlin.test.test

class CdcLockServiceTest {
    private val rLockReactive: RLockReactive = mock()
    private val redissonClient: RedissonReactiveClient = mock()
    private val redisJobLockPolicyConfig: RedisJobLockPolicyConfig =
        RedisJobLockPolicyConfig("lockkeyspace", 20, 2)
    private val cdcLockService: CdcLockService =
        CdcLockService(redissonClient, redisJobLockPolicyConfig)

    /*+ Lock tests **/

    @Test
    fun `Should acquire lock`() {
        // pre-requisites
        val eventId = "eventId"
        given(redissonClient.getLock(any<String>())).willReturn(rLockReactive)
        given(rLockReactive.tryLock(any(), any(), any())).willReturn(mono { true })

        // Test
        cdcLockService.acquireJobLock(eventId).test().expectNext(Unit).verifyComplete()

        // verifications
        verify(redissonClient, times(1)).getLock("lockkeyspace:lock:$eventId")
        verify(rLockReactive, times(1)).tryLock(2, 20, TimeUnit.MILLISECONDS)
    }

    @Test
    fun `Should throw LockNotAcquiredException when lock is already acquired`() {
        // pre-requisites
        val eventId = "eventId"
        given(redissonClient.getLock(any<String>())).willReturn(rLockReactive)
        given(rLockReactive.tryLock(any(), any(), any())).willReturn(Mono.just(false))

        // Test
        cdcLockService
            .acquireJobLock(eventId)
            .test()
            .expectError(LockNotAcquiredException::class.java)
            .verify()

        // verifications
        verify(redissonClient, times(1)).getLock("lockkeyspace:lock:$eventId")
        verify(rLockReactive, times(1)).tryLock(2, 20, TimeUnit.MILLISECONDS)
    }
}
