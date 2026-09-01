package com.atlas.semaforo

fun main() {
    val raw = """
Priority Exclusivo
COP14,027
COP1,230.00/km (estimado)
4.95 (179)
A 11 min (3.3 km)
Viaje: 24 min (8.2 km)
""".trimIndent()

    val offer = OfferParser.parse(raw) ?: error("v0.18 operational offer parse failed")
    val d = SemaforoEngine().evaluate(offer)
    check(offer.fareCop == 14027)
    check(kotlin.math.abs(d.metrics.totalKm - 11.5) < 0.001)
    check(d.metrics.totalMinutes == 35)
    check(kotlin.math.round(d.metrics.copPerKm) == 1220.0)
    check(kotlin.math.round(d.metrics.copPerHour) == 24046.0)
    check(d.band == SemaforoBand.RED)
    println("OperationalOffer14027Parse: PASS")
    println("OperationalOfferEconomics: PASS")
    println("OperationalOfferBandRed: PASS")
}
