package it.pagopa.wallet.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cdc.pay-wallets-log-events")
data class ChangeStreamOptionsConfig(
    val collection: String,
    val operationType: List<String>,
    val project: String
) {}
