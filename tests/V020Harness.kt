package com.atlas.semaforo

fun main() {
    val engine = SemaforoEngine()
    val a = OfferCandidate(14027, 3.3, 11, 8.2, 24, 4.95, null, 1230.0, 98)
    val b = OfferCandidate(10000, 1.0, 5, 3.0, 10, 4.90, null, null, 90)

    val gate1 = StableDecisionGate()
    check(gate1.onOffer(1000, a, engine).decision == null)
    check(gate1.onOffer(1500, a, engine).decision != null)
    val changedOnce = gate1.onOffer(1800, b, engine)
    check(changedOnce.decision == null && !changedOnce.hideOverlay)
    println("ConfirmedOverlaySurvivesSingleChangedRead: PASS")

    val gate2 = StableDecisionGate()
    gate2.onOffer(1000, a, engine)
    check(gate2.onOffer(1500, a, engine).decision != null)
    check(!gate2.onNoOffer(3000).hideOverlay)
    check(!gate2.onNoOffer(7000).hideOverlay)
    println("TransientOcrLossNoFlicker: PASS")

    val gate3 = StableDecisionGate()
    gate3.onOffer(1000, a, engine)
    check(gate3.onOffer(1500, a, engine).decision != null)
    check(!gate3.onOffer(3000, b, engine).hideOverlay)
    val c = b.copy(fareCop = 12500, tripKm = 5.5)
    check(!gate3.onOffer(6000, c, engine).hideOverlay)
    val d = c.copy(fareCop = 15000, tripKm = 7.0)
    check(gate3.onOffer(9600, d, engine).hideOverlay)
    println("StaleOverlayExpiresDuringUnconfirmedCandidates: PASS")
}
