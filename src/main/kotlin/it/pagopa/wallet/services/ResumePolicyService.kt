package it.pagopa.wallet.services

import java.time.Instant

interface ResumePolicyService {
    fun getResumeTimestamp(): Instant
    fun saveResumeTimestamp(timestamp: Instant)
}
