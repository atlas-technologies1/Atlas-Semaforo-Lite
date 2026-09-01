package com.atlas.semaforo

data class GateEvent(val decision: SemaforoDecision?, val hideOverlay: Boolean)

class StableDecisionGate(
    private val requiredReads: Int = 2,
    private val maxGapMs: Long = 2500,
    private val overlayTtlMs: Long = 4000,
    private val fareToleranceCop: Int = 150,
    private val distanceToleranceKm: Double = 0.15,
    private val timeToleranceMin: Int = 1
) {
    private var lastOffer: OfferCandidate? = null
    private var consecutive = 0
    private var lastReadAt = 0L
    private var lastShownAt = 0L
    private var visible = false

    fun onOffer(nowMs: Long, offer: OfferCandidate, engine: SemaforoEngine): GateEvent {
        val p = lastOffer
        val same = p != null &&
            kotlin.math.abs(p.fareCop - offer.fareCop) <= fareToleranceCop &&
            kotlin.math.abs(p.pickupKm - offer.pickupKm) <= distanceToleranceKm &&
            kotlin.math.abs(p.tripKm - offer.tripKm) <= distanceToleranceKm &&
            kotlin.math.abs(p.pickupMin - offer.pickupMin) <= timeToleranceMin &&
            kotlin.math.abs(p.tripMin - offer.tripMin) <= timeToleranceMin &&
            nowMs - lastReadAt <= maxGapMs

        if (!same) {
            lastOffer = offer
            consecutive = 1
            lastReadAt = nowMs
            val oldVisible = visible
            visible = false
            return GateEvent(null, oldVisible)
        }

        lastOffer = offer
        consecutive++
        lastReadAt = nowMs
        if (consecutive >= requiredReads) {
            visible = true
            lastShownAt = nowMs
            return GateEvent(engine.evaluate(offer), false)
        }
        return GateEvent(null, false)
    }

    fun onNoOffer(nowMs: Long): GateEvent {
        if (visible && nowMs - lastShownAt >= overlayTtlMs) {
            visible = false
            consecutive = 0
            lastOffer = null
            return GateEvent(null, true)
        }
        return GateEvent(null, false)
    }
}
