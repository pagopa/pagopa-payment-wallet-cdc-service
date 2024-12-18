package it.pagopa.wallet.services

import it.pagopa.wallet.config.properties.RedisJobLockPolicyConfig
import it.pagopa.wallet.exceptions.LockNotAcquiredException
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlinx.coroutines.reactor.mono
import org.mockito.kotlin.*
import org.redisson.api.RLockReactive
import org.redisson.api.RedissonReactiveClient
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
        cdcLockService.acquireEventLock(eventId).test().expectNext(true).verifyComplete()

        // verifications
        verify(redissonClient, times(1)).getLock("lockkeyspace:lock:$eventId")
        verify(rLockReactive, times(1)).tryLock(2, 20, TimeUnit.MILLISECONDS)
    }

    @Test
    fun `Should throw LockNotAcquiredException when tryLock throw exception`() {
        // pre-requisites
        val eventId = "eventId"
        given(redissonClient.getLock(any<String>())).willReturn(rLockReactive)
        given(rLockReactive.tryLock(any(), any(), any()))
            .willThrow(RuntimeException("Test exception"))

        // Test
        cdcLockService
            .acquireEventLock(eventId)
            .test()
            .expectError(LockNotAcquiredException::class.java)
            .verify()

        // verifications
        verify(redissonClient, times(1)).getLock("lockkeyspace:lock:$eventId")
        verify(rLockReactive, times(1)).tryLock(2, 20, TimeUnit.MILLISECONDS)
    }

    @Test
    fun `Should throw LockNotAcquiredException when getLock throw exception`() {
        // pre-requisites
        val eventId = "eventId"
        given(redissonClient.getLock(any<String>())).willThrow(RuntimeException("Test exception"))

        // Test
        cdcLockService
            .acquireEventLock(eventId)
            .test()
            .expectError(LockNotAcquiredException::class.java)
            .verify()

        // verifications
        verify(redissonClient, times(1)).getLock("lockkeyspace:lock:$eventId")
    }
}
