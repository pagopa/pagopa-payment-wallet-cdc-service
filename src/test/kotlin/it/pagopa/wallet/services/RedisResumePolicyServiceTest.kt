package it.pagopa.wallet.services

import it.pagopa.wallet.config.properties.RedisResumePolicyConfig
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class RedisResumePolicyServiceTest {
    private val redisTemplate: TimestampRedisTemplate = mock()
    private val redisResumePolicyConfig: RedisResumePolicyConfig = mock()
    private lateinit var redisResumePolicyService: ResumePolicyService

    @BeforeEach
    fun initEventStream() {
        redisResumePolicyService = RedisResumePolicyService(redisTemplate, redisResumePolicyConfig)
    }

    @Test
    fun `redis resume policy will get default resume timestamp in case of cache miss`() {
        val emptyMono: Mono<Instant> = mock()
        val expected: Instant = Instant.now()
        given { redisTemplate.findByKeyspaceAndTarget(anyOrNull(), anyOrNull()) }
            .willReturn(emptyMono)
        given { emptyMono.switchIfEmpty(anyOrNull()) }.willReturn(Mono.just(expected))

        StepVerifier.create(redisResumePolicyService.getResumeTimestamp())
            .expectNext(expected)
            .verifyComplete()

        verify(redisTemplate, times(1)).findByKeyspaceAndTarget(anyOrNull(), anyOrNull())
    }

    @Test
    fun `redis resume policy will get resume timestamp in case of cache hit`() {
        val expected: Instant = Instant.now()
        given { redisTemplate.findByKeyspaceAndTarget(anyOrNull(), anyOrNull()) }
            .willReturn(Mono.just(expected))

        StepVerifier.create(redisResumePolicyService.getResumeTimestamp())
            .expectNext(expected)
            .verifyComplete()

        verify(redisTemplate, times(1)).findByKeyspaceAndTarget(anyOrNull(), anyOrNull())
    }

    @Test
    fun `redis resume policy will save resume timestamp`() {
        val expected = true
        given { redisTemplate.save(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()) }
            .willReturn(Mono.just(expected))

        StepVerifier.create(redisResumePolicyService.saveResumeTimestamp(Instant.now()))
            .expectNext(expected)
            .verifyComplete()

        verify(redisTemplate, times(1)).save(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
    }
}
