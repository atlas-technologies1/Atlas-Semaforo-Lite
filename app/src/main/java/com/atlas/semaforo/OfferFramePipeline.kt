package com.atlas.semaforo

import android.graphics.Bitmap
import android.media.Image
import android.os.SystemClock
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

class OfferFramePipeline(private val overlay: SemaforoOverlay) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val busy = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val cadence = FrameCadenceGate(350)
    private val gate = StableDecisionGate()
    private val engine = SemaforoEngine()

    fun onFrame(image: Image) {
        val now = SystemClock.elapsedRealtime()
        if (closed.get() || !cadence.shouldProcess(now) || !busy.compareAndSet(false, true)) return

        val bitmap = imageToBitmap(image) ?: run {
            busy.set(false)
            return
        }

        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                if (closed.get()) return@addOnSuccessListener
                val candidate = OfferParser.parse(result.text)
                val event = if (candidate == null) {
                    gate.onNoOffer(SystemClock.elapsedRealtime())
                } else {
                    gate.onOffer(SystemClock.elapsedRealtime(), candidate, engine)
                }
                if (event.hideOverlay) overlay.hide()
                event.decision?.let { overlay.show(it, candidate?.rating) }
            }
            .addOnFailureListener {
                if (!closed.get()) {
                    val event = gate.onNoOffer(SystemClock.elapsedRealtime())
                    if (event.hideOverlay) overlay.hide()
                }
            }
            .addOnCompleteListener {
                bitmap.recycle()
                busy.set(false)
            }
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        overlay.hide()
        recognizer.close()
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        buffer.rewind()
        val layout = RgbaPlaneLayoutCalculator.calculate(
            width = image.width,
            pixelStride = plane.pixelStride,
            rowStride = plane.rowStride
        ) ?: return null

        val padded = Bitmap.createBitmap(layout.paddedWidth, image.height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(buffer)
        val cropped = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
        if (cropped !== padded) padded.recycle()
        return cropped
    }
}
