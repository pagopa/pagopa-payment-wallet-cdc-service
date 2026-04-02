package it.pagopa.wallet.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.time.Instant
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    fun reactiveRedisTemplate(
        reactiveRedisConnectionFactory: ReactiveRedisConnectionFactory
    ): ReactiveRedisTemplate<String, Instant> {
        val mapper =
            ObjectMapper().apply {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }

        val keySerializer = StringRedisSerializer()
        val valueSerializer: Jackson2JsonRedisSerializer<Instant> =
            Jackson2JsonRedisSerializer(mapper, Instant::class.java)

        val serializationContext =
            RedisSerializationContext.newSerializationContext<String, Instant>(keySerializer)
                .value(valueSerializer)
                .build()

        return ReactiveRedisTemplate(reactiveRedisConnectionFactory, serializationContext)
    }
}
