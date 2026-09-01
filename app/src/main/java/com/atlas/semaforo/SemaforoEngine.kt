package com.atlas.semaforo

enum class SemaforoBand { GREEN, YELLOW, RED }

data class SemaforoPolicy(
    val greenCopPerKm: Double = 2300.0,
    val yellowCopPerKm: Double = 1700.0,
    val greenCopPerHour: Double = 30000.0,
    val yellowCopPerHour: Double = 22000.0
)

data class EconomicMetrics(
    val copPerKm: Double,
    val copPerHour: Double,
    val totalKm: Double,
    val totalMinutes: Int
)

data class SemaforoDecision(
    val band: SemaforoBand,
    val metrics: EconomicMetrics,
    val confidence: Int
)

class SemaforoEngine(private val policy: SemaforoPolicy = SemaforoPolicy()) {
    fun evaluate(offer: OfferCandidate): SemaforoDecision {
        val totalKm = offer.pickupKm + offer.tripKm
        val totalMinutes = offer.pickupMin + offer.tripMin
        require(totalKm > 0.0 && totalMinutes > 0)
        val copPerKm = offer.fareCop / totalKm
        val copPerHour = offer.fareCop * 60.0 / totalMinutes
        val band = when {
            copPerKm >= policy.greenCopPerKm && copPerHour >= policy.greenCopPerHour -> SemaforoBand.GREEN
            copPerKm >= policy.yellowCopPerKm && copPerHour >= policy.yellowCopPerHour -> SemaforoBand.YELLOW
            else -> SemaforoBand.RED
        }
        return SemaforoDecision(
            band,
            EconomicMetrics(copPerKm, copPerHour, totalKm, totalMinutes),
            offer.confidence
        )
    }
}
