package it.pagopa.wallet.services

import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class TimeStampRedisTemplateTest {
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Instant> = mock()
    private val valueOps: ReactiveValueOperations<String, Instant> = mock()
    private lateinit var timestampRedisTemplate: TimestampRedisTemplate

    @BeforeEach
    fun initTimeStampRedisTemplate() {
        timestampRedisTemplate = TimestampRedisTemplate(reactiveRedisTemplate)
    }

    @Test
    fun `time stamp redis template saves instant`() {
        val expected = true
        given { reactiveRedisTemplate.opsForValue() }.willReturn(valueOps)
        given { valueOps.set(anyOrNull(), anyOrNull(), any<Duration>()) }
            .willReturn(Mono.just(expected))

        StepVerifier.create(
                timestampRedisTemplate.save(
                    "keyspace",
                    "target",
                    Instant.now(),
                    Duration.ofSeconds(0)
                )
            )
            .expectNext(expected)
            .verifyComplete()

        verify(reactiveRedisTemplate, times(1)).opsForValue()
        verify(valueOps, times(1)).set(anyOrNull(), anyOrNull(), any<Duration>())
    }

    @Test
    fun `time stamp redis template gets instant`() {
        val expected = Instant.now()
        given { reactiveRedisTemplate.opsForValue() }.willReturn(valueOps)
        given { valueOps.get(anyOrNull()) }.willReturn(Mono.just(expected))

        StepVerifier.create(timestampRedisTemplate.findByKeyspaceAndTarget("keyspace", "target"))
            .expectNext(expected)
            .verifyComplete()

        verify(reactiveRedisTemplate, times(1)).opsForValue()
        verify(valueOps, times(1)).get(anyOrNull())
    }
}
