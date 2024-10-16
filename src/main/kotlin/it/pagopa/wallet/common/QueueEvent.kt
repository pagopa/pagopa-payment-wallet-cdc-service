package it.pagopa.wallet.common

import it.pagopa.wallet.common.tracing.QueueTracingInfo
import org.bson.Document

data class QueueEvent<T : Document>(val data: T, val tracingInfo: QueueTracingInfo)
