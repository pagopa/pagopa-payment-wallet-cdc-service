package it.pagopa.wallet.common

import java.time.OffsetDateTime

/** Redis value stored for a CDC event lock: identifies who acquired the lock and when. */
data class ExclusiveLockDocument(
    val id: String,
    val holderName: String,
    val creationDate: String = OffsetDateTime.now().toString()
)
