package com.atlas.semaforo

class FrameCadenceGate(private val minIntervalMs: Long = 350) {
    private var lastAccepted = Long.MIN_VALUE
    fun shouldProcess(nowMs: Long): Boolean {
        if (lastAccepted == Long.MIN_VALUE || nowMs - lastAccepted >= minIntervalMs) {
            lastAccepted = nowMs
            return true
        }
        return false
    }
}
