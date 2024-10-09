package it.pagopa.wallet.services

import it.pagopa.wallet.config.RedisResumePolicyConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import java.util.*

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
    fun `redis resume policy will get default resume timestamp in case of cache miss`(){
        val emptyOptional: Optional<Instant> = mock()
        val expected: Instant = Instant.now()
        given{redisTemplate.findByKeyspaceAndTarget(anyOrNull(), anyOrNull())}.willReturn(emptyOptional)
        given{emptyOptional.orElseGet(anyOrNull())}.willReturn(expected)


        val actual = redisResumePolicyService.getResumeTimestamp()
        Assertions.assertTrue(actual == expected)
    }

    @Test
    fun `redis resume policy will get resume timestamp in case of cache hit`(){
        val expected: Instant = Instant.now()
        given{redisTemplate.findByKeyspaceAndTarget(anyOrNull(), anyOrNull())}.willReturn(Optional.of(expected))


        val actual = redisResumePolicyService.getResumeTimestamp()
        Assertions.assertTrue(actual == expected)
    }
}