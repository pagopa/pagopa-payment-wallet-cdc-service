package it.pagopa.wallet.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cdc.retry-send")
data class RetrySendPolicyConfig(val maxAttempts: Long, val intervalInMillis: Long) {}
