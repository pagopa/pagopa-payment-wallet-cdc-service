package it.pagopa.wallet

import it.pagopa.wallet.config.properties.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import reactor.core.publisher.Hooks

@SpringBootApplication
@EnableConfigurationProperties(
    CdcQueueConfig::class,
    ChangeStreamOptionsConfig::class,
    RetrySendPolicyConfig::class,
    RedisResumePolicyConfig::class,
    RetryStreamPolicyConfig::class
)
class PagopaPaymentWalletCdcServiceApplication

fun main(args: Array<String>) {
    Hooks.enableAutomaticContextPropagation()
    runApplication<PagopaPaymentWalletCdcServiceApplication>(*args)
}
