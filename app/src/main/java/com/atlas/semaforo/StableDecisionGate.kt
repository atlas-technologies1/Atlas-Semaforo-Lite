package com.atlas.semaforo

data class GateEvent(val decision: SemaforoDecision?, val hideOverlay: Boolean)

class StableDecisionGate(
    private val requiredReads: Int = 2,
    private val maxGapMs: Long = 2500,
    private val noOfferGraceMs: Long = 1200,
    private val overlayTtlMs: Long = 6000,
    private val fastPathConfidence: Int = 95,
    private val fareToleranceCop: Int = 150,
    private val distanceToleranceKm: Double = 0.15,
    private val timeToleranceMin: Int = 1
) {
    private var lastOffer: OfferCandidate? = null
    private var consecutive = 0
    private var lastReadAt = 0L
    private var lastValidOfferAt = 0L
    private var lastShownAt = 0L
    private var visible = false

    fun onOffer(nowMs: Long, offer: OfferCandidate, engine: SemaforoEngine): GateEvent {
        lastValidOfferAt = nowMs

        val ttlExpired = visible && nowMs - lastShownAt >= overlayTtlMs
        if (ttlExpired) visible = false

        val previous = lastOffer
        val same = previous != null &&
            kotlin.math.abs(previous.fareCop - offer.fareCop) <= fareToleranceCop &&
            kotlin.math.abs(previous.pickupKm - offer.pickupKm) <= distanceToleranceKm &&
            kotlin.math.abs(previous.tripKm - offer.tripKm) <= distanceToleranceKm &&
            kotlin.math.abs(previous.pickupMin - offer.pickupMin) <= timeToleranceMin &&
            kotlin.math.abs(previous.tripMin - offer.tripMin) <= timeToleranceMin &&
            nowMs - lastReadAt <= maxGapMs

        if (!same) {
            lastOffer = offer
            consecutive = 1
            lastReadAt = nowMs

            // v0.22 fast path: when fare + pickup + trip + Uber displayed COP/km
            // agree strongly, one OCR read is already cross-validated by the parser.
            if (offer.confidence >= fastPathConfidence) {
                visible = true
                lastShownAt = nowMs
                return GateEvent(engine.evaluate(offer), false)
            }

            // Keep an already confirmed card during one noisy changed OCR read.
            return GateEvent(null, ttlExpired)
        }

        lastOffer = offer
        consecutive++
        lastReadAt = nowMs
        if (consecutive >= requiredReads) {
            visible = true
            lastShownAt = nowMs
            return GateEvent(engine.evaluate(offer), false)
        }
        return GateEvent(null, ttlExpired)
    }

    fun onNoOffer(nowMs: Long): GateEvent {
        if (!visible) {
            if (lastValidOfferAt != 0L && nowMs - lastValidOfferAt >= noOfferGraceMs) {
                resetCandidate()
            }
            return GateEvent(null, false)
        }

        // The Uber offer panel closing should clear Atlas quickly, but a single OCR
        // miss must not cause flicker. 1.2 s is ~3 capture cadences at 350 ms.
        val offerGone = lastValidOfferAt != 0L && nowMs - lastValidOfferAt >= noOfferGraceMs
        val ttlExpired = nowMs - lastShownAt >= overlayTtlMs
        if (offerGone || ttlExpired) {
            visible = false
            resetCandidate()
            return GateEvent(null, true)
        }
        return GateEvent(null, false)
    }

    private fun resetCandidate() {
        consecutive = 0
        lastOffer = null
        lastReadAt = 0L
        lastValidOfferAt = 0L
    }
}
