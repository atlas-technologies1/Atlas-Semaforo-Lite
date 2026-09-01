package com.atlas.semaforo

fun main() {
    val raw = """
UberYa Exclusivo
COP3,379.00/km (estimado)
COP6,082
4.85 (334)
A 3 min (0.7 km)
Viaje: 5 min (1.1 km)
""".trimIndent()

    val offer = OfferParser.parse(raw) ?: error("parse failed")
    val engine = SemaforoEngine()
    val decision = engine.evaluate(offer)

    check(offer.fareCop == 6082)
    check(kotlin.math.round(decision.metrics.copPerKm) == 3379.0)
    check(kotlin.math.round(decision.metrics.copPerHour) == 45615.0)
    check(decision.band == SemaforoBand.GREEN)

    val contradiction = """
COP6,082
COP1,000.00/km
A 3 min (0.7 km)
Viaje: 5 min (1.1 km)
""".trimIndent()
    check(OfferParser.parse(contradiction) == null)

    val gate = StableDecisionGate()
    check(gate.onOffer(1000, offer, engine).decision == null)
    check(gate.onOffer(1600, offer.copy(fareCop = 6090), engine).decision != null)

    val changed = offer.copy(fareCop = 9000, tripKm = 3.0)
    val changedEvent = gate.onOffer(1900, changed, engine)
    check(changedEvent.decision == null && !changedEvent.hideOverlay)
    check(gate.onOffer(2300, changed, engine).decision != null)
    check(gate.onNoOffer(10400).hideOverlay)

    val cadence = FrameCadenceGate()
    check(cadence.shouldProcess(1000))
    check(!cadence.shouldProcess(1200))
    check(cadence.shouldProcess(1350))


    val exactLayout = RgbaPlaneLayoutCalculator.calculate(1080, 4, 4320)
    check(exactLayout?.paddedWidth == 1080)
    val paddedLayout = RgbaPlaneLayoutCalculator.calculate(1080, 4, 4352)
    check(paddedLayout?.paddedWidth == 1088)
    check(RgbaPlaneLayoutCalculator.calculate(1080, 3, 3240) == null)
    check(RgbaPlaneLayoutCalculator.calculate(1080, 4, 4319) == null)

    println("RgbaPlaneLayoutValidation: PASS")
    println("RealOfferParse: PASS")
    println("Economics: PASS")
    println("DisplayedRateFailClosed: PASS")
    println("TwoReadToleranceConsensus: PASS")
    println("ChangedOfferInvalidation: PASS")
    println("OverlayTTL: PASS")
    println("FrameCadenceThrottle: PASS")
}
