package it.pagopa.wallet.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cdc.retry-stream")
data class RetryStreamPolicyConfig(val maxAttempts: Long, val intervalInMs: Long) {}
