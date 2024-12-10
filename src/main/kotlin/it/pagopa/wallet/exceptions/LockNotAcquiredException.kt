package it.pagopa.wallet.exceptions

class LockNotAcquiredException(lockName: String, throwable: Throwable? = null) :
    LockException("Could not acquire the lock [${lockName}]", throwable)
