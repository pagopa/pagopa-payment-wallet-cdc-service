package it.pagopa.wallet.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("cdc-queue")
data class CdcQueueConfig(
    val storageConnectionString: String,
    val storageQueueName: String,
    val ttlSeconds: Long,
    val timeoutWalletExpired: Long
)
