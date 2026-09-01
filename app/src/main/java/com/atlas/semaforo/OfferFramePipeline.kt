package com.atlas.semaforo

import android.graphics.Bitmap
import android.media.Image
import android.os.SystemClock
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

class OfferFramePipeline(
    private val overlay: SemaforoOverlay,
    private val engine: SemaforoEngine
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val busy = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val cadence = FrameCadenceGate(350)
    private val gate = StableDecisionGate()
    private val serviceMemory = ServiceMemoryTracker()

    fun onFrame(image: Image) {
        val now = SystemClock.elapsedRealtime()
        if (closed.get() || !cadence.shouldProcess(now) || !busy.compareAndSet(false, true)) return
        val bitmap = imageToBitmap(image) ?: run { busy.set(false); return }

        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                if (closed.get()) return@addOnSuccessListener
                val ts = SystemClock.elapsedRealtime()
                val candidate = OfferParser.parse(result.text)
                val gateEvent = if (candidate == null) gate.onNoOffer(ts) else gate.onOffer(ts, candidate, engine)
                if (gateEvent.hideOverlay) overlay.hideOffer()
                gateEvent.decision?.let { overlay.show(it, candidate?.rating) }

                val signal = UberScreenSignalClassifier.classify(result.text, candidate != null)
                val memoryEvent = serviceMemory.onFrame(ts, candidate, gateEvent.decision, signal)
                if (memoryEvent.becameAccepted) memoryEvent.activeService?.let { overlay.showActive(it) }
                if (memoryEvent.becameFinished) overlay.clearActive()
            }
            .addOnFailureListener {
                if (!closed.get()) {
                    val event = gate.onNoOffer(SystemClock.elapsedRealtime())
                    if (event.hideOverlay) overlay.hideOffer()
                    // OCR failures never mutate acceptance/finish memory: avoid false state changes.
                }
            }
            .addOnCompleteListener { bitmap.recycle(); busy.set(false) }
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        serviceMemory.clear(); overlay.hideAll(); recognizer.close()
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer; buffer.rewind()
        val layout = RgbaPlaneLayoutCalculator.calculate(image.width, plane.pixelStride, plane.rowStride) ?: return null
        val padded = Bitmap.createBitmap(layout.paddedWidth, image.height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(buffer)
        val cropped = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
        if (cropped !== padded) padded.recycle()
        return cropped
    }
}
