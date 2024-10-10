package it.pagopa.wallet.common

import it.pagopa.wallet.common.tracing.QueueTracingInfo

data class QueueEvent<T : String>(val data: T, val tracingInfo: QueueTracingInfo)
