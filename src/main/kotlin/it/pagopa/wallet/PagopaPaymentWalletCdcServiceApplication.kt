package it.pagopa.wallet

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import reactor.core.publisher.Hooks

@SpringBootApplication class PagopaPaymentWalletCdcServiceApplication

fun main(args: Array<String>) {
    Hooks.enableAutomaticContextPropagation()
    runApplication<PagopaPaymentWalletCdcServiceApplication>(*args)
}
