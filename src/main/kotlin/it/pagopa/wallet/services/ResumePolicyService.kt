package it.pagopa.wallet.services

import reactor.core.publisher.Mono
import java.time.Instant

interface ResumePolicyService {
    fun getResumeTimestamp(): Mono<Instant>

    fun saveResumeTimestamp(timestamp: Instant): Mono<Boolean>
}
