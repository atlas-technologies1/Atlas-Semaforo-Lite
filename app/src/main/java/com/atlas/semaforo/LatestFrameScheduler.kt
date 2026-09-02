package com.atlas.semaforo

class LatestFrameScheduler<T>(
    private val dispose: (T) -> Unit
) {
    private var running = false
    private var pending: T? = null

    @Synchronized
    fun submit(frame: T): SubmitResult<T> {
        if (!running) {
            running = true
            return SubmitResult.StartNow(frame)
        }
        val old = pending
        pending = frame
        if (old != null) dispose(old)
        return SubmitResult.Coalesced
    }

    @Synchronized
    fun completeAndTakeNext(): T? {
        val next = pending
        pending = null
        if (next == null) {
            running = false
            return null
        }
        return next
    }

    @Synchronized
    fun clear() {
        pending?.let(dispose)
        pending = null
        running = false
    }

    sealed class SubmitResult<out T> {
        data class StartNow<T>(val frame: T) : SubmitResult<T>()
        data object Coalesced : SubmitResult<Nothing>()
    }
}
