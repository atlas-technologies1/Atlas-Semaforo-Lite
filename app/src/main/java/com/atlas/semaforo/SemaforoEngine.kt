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
        hardFloorCopPerKm > 0.0 &&
        minimumCopPerKm >= hardFloorCopPerKm &&
        excellentCopPerKm >= minimumCopPerKm &&
        hardFloorCopPerHour > 0.0 &&
        minimumCopPerHour >= hardFloorCopPerHour &&
        excellentCopPerHour >= minimumCopPerHour &&
        minimumRating in 1.0..5.0 &&
        excellentRating in 1.0..5.0 &&
        excellentRating >= minimumRating
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
    fun evaluate(offer: OfferCandidate): SemaforoDecision {
        val totalKm = offer.pickupKm + offer.tripKm
        val totalMinutes = offer.pickupMin + offer.tripMin
        require(totalKm > 0.0 && totalMinutes > 0)
        val cpkm = offer.fareCop / totalKm
        val cph = offer.fareCop * 60.0 / totalMinutes
        val ks = scoreMetric(cpkm, policy.hardFloorCopPerKm, policy.minimumCopPerKm, policy.excellentCopPerKm)
        val hs = scoreMetric(cph, policy.hardFloorCopPerHour, policy.minimumCopPerHour, policy.excellentCopPerHour)
        val rs = if (policy.ratingEnabled && offer.rating != null) scoreRating(offer.rating) else null
        val score = (0.45 * ks + 0.45 * hs + 0.10 * (rs ?: 70)).roundToInt().coerceIn(0, 100)
        val bothLow = cpkm < policy.minimumCopPerKm && cph < policy.minimumCopPerHour
        val hardFloor = cpkm < policy.hardFloorCopPerKm || cph < policy.hardFloorCopPerHour
        val ratingLow = policy.ratingEnabled && offer.rating != null && offer.rating < policy.minimumRating

        var band = when {
            bothLow -> SemaforoBand.RED
            score >= 80 -> SemaforoBand.GREEN
            score >= 60 -> SemaforoBand.YELLOW
            else -> SemaforoBand.RED
        }
        if ((hardFloor || ratingLow) && band == SemaforoBand.GREEN) band = SemaforoBand.YELLOW

        val reason = when {
            bothLow -> "km y hora bajo mínimo"
            hardFloor -> "piso económico activado"
            hs >= ks + 15 -> "hora compensa km"
            ks >= hs + 15 -> "km compensa hora"
            else -> "rentabilidad equilibrada"
        }

        return SemaforoDecision(
            band, EconomicMetrics(cpkm, cph, totalKm, totalMinutes),
            offer.confidence, score, ComponentScores(ks, hs, rs), reason
        )
    }

    private fun scoreMetric(v: Double, floor: Double, min: Double, excellent: Double): Int = when {
        v <= floor -> 0.0
        v < min -> lerp(v, floor, min, 0.0, 60.0)
        v < excellent -> lerp(v, min, excellent, 60.0, 100.0)
        else -> 100.0
    }.roundToInt().coerceIn(0, 100)

    private fun scoreRating(r: Double): Int = when {
        r < policy.minimumRating -> 20
        r < policy.excellentRating -> lerp(r, policy.minimumRating, policy.excellentRating, 60.0, 100.0).roundToInt()
        else -> 100
    }.coerceIn(0, 100)

    private fun lerp(v: Double, a: Double, b: Double, oa: Double, ob: Double): Double =
        if (b <= a) ob else oa + ((v - a) / (b - a)).coerceIn(0.0, 1.0) * (ob - oa)
}
