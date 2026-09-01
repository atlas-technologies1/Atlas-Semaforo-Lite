package com.atlas.semaforo

data class CaptureSize(val width: Int, val height: Int)

object ProjectionSurfacePolicy {
    fun normalized(width: Int, height: Int): CaptureSize? {
        if (width <= 0 || height <= 0) return null
        if (width > 16384 || height > 16384) return null
        return CaptureSize(width, height)
    }

    fun needsResize(current: CaptureSize?, requested: CaptureSize): Boolean =
        current == null || current.width != requested.width || current.height != requested.height
}
