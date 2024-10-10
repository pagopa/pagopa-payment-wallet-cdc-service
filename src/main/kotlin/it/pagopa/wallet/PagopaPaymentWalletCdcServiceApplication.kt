package it.pagopa.wallet

import it.pagopa.wallet.config.ChangeStreamOptionsConfig
import it.pagopa.wallet.config.RetrySendPolicyConfig
import it.pagopa.wallet.config.RedisResumePolicyConfig
import it.pagopa.wallet.config.properties.CdcQueueConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import reactor.core.publisher.Hooks

@SpringBootApplication
@EnableConfigurationProperties(
    CdcQueueConfig::class,
    ChangeStreamOptionsConfig::class,
    RetrySendPolicyConfig::class,
    RedisResumePolicyConfig::class
)
class PagopaPaymentWalletCdcServiceApplication

fun main(args: Array<String>) {
    Hooks.enableAutomaticContextPropagation()
    runApplication<PagopaPaymentWalletCdcServiceApplication>(*args)
}
