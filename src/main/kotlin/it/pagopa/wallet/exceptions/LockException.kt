package it.pagopa.wallet.exceptions

sealed class LockException(message: String, throwable: Throwable? = null) :
    RuntimeException(message, throwable)
