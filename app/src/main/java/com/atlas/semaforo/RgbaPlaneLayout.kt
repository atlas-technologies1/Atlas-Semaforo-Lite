package com.atlas.semaforo

data class RgbaPlaneLayout(val paddedWidth: Int, val rowPaddingBytes: Int)

object RgbaPlaneLayoutCalculator {
    fun calculate(width: Int, pixelStride: Int, rowStride: Int): RgbaPlaneLayout? {
        if (width <= 0 || pixelStride != 4) return null
        val minimum = width * pixelStride
        if (rowStride < minimum) return null
        val padding = rowStride - minimum
        if (padding % pixelStride != 0) return null
        return RgbaPlaneLayout(
            paddedWidth = width + padding / pixelStride,
            rowPaddingBytes = padding
        )
    }
}
