package com.atlas.semaforo

data class GateEvent(val decision: SemaforoDecision?, val hideOverlay: Boolean)

class StableDecisionGate(
    private val requiredReads: Int = 1,
    private val maxGapMs: Long = 2500,
    private val noOfferGraceMs: Long = 1200,
    private val overlayTtlMs: Long = 6000,
    private val fastPathConfidence: Int = 90,
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
        } else {
            consecutive++
            lastReadAt = nowMs
            lastOffer = offer
        }

        if (requiredReads <= 1 || offer.confidence >= fastPathConfidence || consecutive >= requiredReads) {
            visible = true
            lastShownAt = nowMs
            return GateEvent(engine.evaluate(offer), false)
        }
        return GateEvent(null, false)
    }

    fun onNoOffer(nowMs: Long): GateEvent {
        if (!visible) {
            if (lastValidOfferAt != 0L && nowMs - lastValidOfferAt >= noOfferGraceMs) resetCandidate()
            return GateEvent(null, false)
        }

        val gone = lastValidOfferAt != 0L && nowMs - lastValidOfferAt >= noOfferGraceMs
        val ttl = nowMs - lastShownAt >= overlayTtlMs
        if (gone || ttl) {
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
