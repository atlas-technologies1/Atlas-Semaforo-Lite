package com.atlas.semaforo

import kotlin.math.roundToInt

enum class SemaforoBand { GREEN, YELLOW, RED }

data class SemaforoPolicy(
    val excellentCopPerKm: Double = 2200.0,
    val minimumCopPerKm: Double = 1500.0,
    val hardFloorCopPerKm: Double = 1200.0,
    val excellentCopPerHour: Double = 35000.0,
    val minimumCopPerHour: Double = 25000.0,
    val hardFloorCopPerHour: Double = 22000.0,
    val ratingEnabled: Boolean = true,
    val excellentRating: Double = 4.85,
    val minimumRating: Double = 4.60
) {
    fun isValid(): Boolean =
        hardFloorCopPerKm > 0.0 && minimumCopPerKm >= hardFloorCopPerKm && excellentCopPerKm >= minimumCopPerKm &&
        hardFloorCopPerHour > 0.0 && minimumCopPerHour >= hardFloorCopPerHour && excellentCopPerHour >= minimumCopPerHour &&
        minimumRating in 1.0..5.0 && excellentRating in 1.0..5.0 && excellentRating >= minimumRating
}

data class EconomicMetrics(val copPerKm: Double, val copPerHour: Double, val totalKm: Double, val totalMinutes: Int)
data class ComponentScores(val km: Int, val hour: Int, val rating: Int?)
data class SemaforoDecision(
    val band: SemaforoBand,
    val metrics: EconomicMetrics,
    val confidence: Int,
    val economicScore: Int,
    val components: ComponentScores,
    val reason: String
)

class SemaforoEngine(private val policy: SemaforoPolicy = SemaforoPolicy()) {
    init { require(policy.isValid()) }

    fun evaluate(offer: OfferCandidate): SemaforoDecision {
        val totalKm = offer.pickupKm + offer.tripKm
        val totalMinutes = offer.pickupMin + offer.tripMin
        require(totalKm > 0.0 && totalMinutes > 0)
        val copPerKm = offer.fareCop / totalKm
        val copPerHour = offer.fareCop * 60.0 / totalMinutes

        val kmScore = scoreMetric(copPerKm, policy.hardFloorCopPerKm, policy.minimumCopPerKm, policy.excellentCopPerKm)
        val hourScore = scoreMetric(copPerHour, policy.hardFloorCopPerHour, policy.minimumCopPerHour, policy.excellentCopPerHour)
        val ratingScore = if (policy.ratingEnabled && offer.rating != null) scoreRating(offer.rating) else null
        val ratingContribution = ratingScore ?: 70 // neutral when rating is absent/disabled
        var score = (0.45 * kmScore + 0.45 * hourScore + 0.10 * ratingContribution).roundToInt().coerceIn(0, 100)

        val bothBelowMinimum = copPerKm < policy.minimumCopPerKm && copPerHour < policy.minimumCopPerHour
        val hardFloorHit = copPerKm < policy.hardFloorCopPerKm || copPerHour < policy.hardFloorCopPerHour
        val ratingHardFail = policy.ratingEnabled && offer.rating != null && offer.rating < policy.minimumRating

        var band = when {
            bothBelowMinimum -> SemaforoBand.RED
            score >= 80 -> SemaforoBand.GREEN
            score >= 60 -> SemaforoBand.YELLOW
            else -> SemaforoBand.RED
        }
        if (hardFloorHit && band == SemaforoBand.GREEN) band = SemaforoBand.YELLOW
        if (ratingHardFail && band == SemaforoBand.GREEN) band = SemaforoBand.YELLOW

        val reason = when {
            bothBelowMinimum -> "km y hora bajo mínimo"
            hardFloorHit -> "piso económico activado"
            hourScore >= kmScore + 15 -> "hora compensa km"
            kmScore >= hourScore + 15 -> "km compensa hora"
            else -> "rentabilidad equilibrada"
        }

        return SemaforoDecision(band, EconomicMetrics(copPerKm, copPerHour, totalKm, totalMinutes), offer.confidence,
            score, ComponentScores(kmScore, hourScore, ratingScore), reason)
    }

    private fun scoreMetric(value: Double, floor: Double, minimum: Double, excellent: Double): Int = when {
        value <= floor -> 0.0
        value < minimum -> lerp(value, floor, minimum, 0.0, 60.0)
        value < excellent -> lerp(value, minimum, excellent, 60.0, 100.0)
        else -> 100.0
    }.roundToInt().coerceIn(0, 100)

    private fun scoreRating(rating: Double): Int = when {
        rating < policy.minimumRating -> 20
        rating < policy.excellentRating -> lerp(rating, policy.minimumRating, policy.excellentRating, 60.0, 100.0).roundToInt()
        else -> 100
    }.coerceIn(0, 100)

    private fun lerp(v: Double, a: Double, b: Double, outA: Double, outB: Double): Double {
        if (b <= a) return outB
        return outA + ((v - a) / (b - a)).coerceIn(0.0, 1.0) * (outB - outA)
    }
}
