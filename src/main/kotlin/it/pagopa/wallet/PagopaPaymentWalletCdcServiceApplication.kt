package it.pagopa.wallet

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PagopaPaymentWalletCdcServiceApplication

fun main(args: Array<String>) {
	runApplication<PagopaPaymentWalletCdcServiceApplication>(*args)
}
