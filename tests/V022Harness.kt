package com.atlas.semaforo

private fun checkV22(name: String, condition: Boolean) {
    if (!condition) error("FAIL: $name")
    println("$name: PASS")
}

fun main() {
    val engine = SemaforoEngine()
    val high = OfferCandidate(25045, 1.0, 6, 8.7, 29, 4.93, null, 2582.0, 98)
    val low = OfferCandidate(16045, 1.8, 11, 11.6, 37, 4.87, null, 1189.0, 95)

    run {
        val g = StableDecisionGate()
        val e = g.onOffer(1000, high, engine)
        checkV22("HighConfidenceFirstReadShows", e.decision != null && !e.hideOverlay)
    }

    run {
        val g = StableDecisionGate()
        checkV22("LowOfferFastPathShows", g.onOffer(1000, low, engine).decision != null)
        checkV22("SingleOcrMissDoesNotHide", !g.onNoOffer(1500).hideOverlay)
        checkV22("ClosedOfferHidesQuickly", g.onNoOffer(2301).hideOverlay)
    }

    val textGreen = """
        Priority Exclusivo
        COP25,045
        COP2,582.00/km (estimado)
        4.93 (193)
        +COP2,212.00 por inicio de viaje
        A 6 min (1.0 km)
        Viaje: 29 min (8.7 km)
    """.trimIndent()
    val parsedGreen = OfferParser.parse(textGreen)
    checkV22("GreenPhysicalFixtureParse", parsedGreen != null && parsedGreen.rating == 4.93 && parsedGreen.confidence == 98)

    val textStandaloneRating = """
        UberYa Exclusivo
        COP8,019
        COP1,782.00/km (estimado)
        4.92 +COP2,100.00
        A 2 min (0.3 km)
        Viaje: 13 min (4.2 km)
    """.trimIndent()
    val parsedStandalone = OfferParser.parse(textStandaloneRating)
    checkV22("StandaloneRatingParse", parsedStandalone?.rating == 4.92)

    val noRating = """
        UberYa Exclusivo
        COP16,045
        COP1,189.00/km (estimado)
        A 11 min (1.8 km)
        Viaje: 37 min (11.6 km)
    """.trimIndent()
    val parsedNoRating = OfferParser.parse(noRating)
    checkV22("NoFalseKmRating", parsedNoRating != null && parsedNoRating.rating == null)
}
