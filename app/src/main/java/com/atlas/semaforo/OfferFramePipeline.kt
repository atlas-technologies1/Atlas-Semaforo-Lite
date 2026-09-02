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
    private val closed = AtomicBoolean(false)
    private val cadence = FrameCadenceGate(180)
    private val assembler = IncrementalOfferAssembler()
    private val gate = StableDecisionGate()
    private val serviceMemory = ServiceMemoryTracker()

    private val scheduler = LatestFrameScheduler<Bitmap> {
        if (!it.isRecycled) it.recycle()
    }

    fun onFrame(image: Image) {
        if (closed.get()) return
        val now = SystemClock.elapsedRealtime()
        if (!cadence.shouldProcess(now)) return
        val bitmap = imageToBitmap(image) ?: return
        when (val result = scheduler.submit(bitmap)) {
            is LatestFrameScheduler.SubmitResult.StartNow -> processBitmap(result.frame)
            LatestFrameScheduler.SubmitResult.Coalesced -> Unit
        }
    }

    private fun processBitmap(bitmap: Bitmap) {
        if (closed.get()) {
            if (!bitmap.isRecycled) bitmap.recycle()
            return
        }
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                if (closed.get()) return@addOnSuccessListener
                val ts = SystemClock.elapsedRealtime()
                val observation = OfferParser.observe(result.text)
                val candidate = assembler.ingest(ts, observation)
                val event = if (candidate == null) {
                    assembler.onNoOffer(ts)
                    gate.onNoOffer(ts)
                } else gate.onOffer(ts, candidate, engine)

                if (event.hideOverlay) overlay.hideOffer()
                event.decision?.let { overlay.show(it, candidate?.rating) }

                val signal = UberScreenSignalClassifier.classify(result.text, candidate != null)
                val memoryEvent = serviceMemory.onFrame(ts, candidate, event.decision, signal)
                if (memoryEvent.becameAccepted)
                    memoryEvent.activeService?.let { overlay.showActive(it) }
                if (memoryEvent.becameFinished) overlay.clearActive()
            }
            .addOnFailureListener {
                if (!closed.get()) {
                    val ts = SystemClock.elapsedRealtime()
                    assembler.onNoOffer(ts)
                    val event = gate.onNoOffer(ts)
                    if (event.hideOverlay) overlay.hideOffer()
                }
            }
            .addOnCompleteListener {
                if (!bitmap.isRecycled) bitmap.recycle()
                scheduler.completeAndTakeNext()?.let { processBitmap(it) }
            }
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        scheduler.clear()
        assembler.clear()
        serviceMemory.clear()
        overlay.hideAll()
        recognizer.close()
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        buffer.rewind()
        val layout = RgbaPlaneLayoutCalculator.calculate(
            image.width, plane.pixelStride, plane.rowStride
        ) ?: return null
        val padded = Bitmap.createBitmap(
            layout.paddedWidth, image.height, Bitmap.Config.ARGB_8888
        )
        padded.copyPixelsFromBuffer(buffer)
        val cropped = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
        if (cropped !== padded) padded.recycle()
        return cropped
    }
}
