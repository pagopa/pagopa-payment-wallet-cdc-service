package it.pagopa.wallet

import it.pagopa.wallet.config.ChangeStreamOptionsConfig
import it.pagopa.wallet.config.RedisResumePolicyConfig
import it.pagopa.wallet.config.RetrySendPolicyConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
    ChangeStreamOptionsConfig::class,
    RetrySendPolicyConfig::class,
    RedisResumePolicyConfig::class
)
class PagopaPaymentWalletCdcServiceApplication

fun main(args: Array<String>) {
    runApplication<PagopaPaymentWalletCdcServiceApplication>(*args)
}
