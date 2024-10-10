package it.pagopa.wallet.common

import it.pagopa.wallet.common.tracing.QueueTracingInfo
import org.bson.BsonDocument

data class QueueEvent<T : BsonDocument>(val data: T, val tracingInfo: QueueTracingInfo)
