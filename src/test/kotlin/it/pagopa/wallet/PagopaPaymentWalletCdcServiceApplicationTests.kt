package it.pagopa.wallet

import it.pagopa.wallet.services.TimestampRedisTemplate
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(locations = ["classpath:application-test.properties"])
class PagopaPaymentWalletCdcServiceApplicationTests {
    @MockBean private lateinit var mockRedisTemplate: TimestampRedisTemplate

    @Test
    fun contextLoads() {
        // check only if the context is loaded
        Assertions.assertTrue(true)
    }
}
