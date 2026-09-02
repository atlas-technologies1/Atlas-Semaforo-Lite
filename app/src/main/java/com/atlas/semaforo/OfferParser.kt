package com.atlas.semaforo

data class OfferCandidate(
    val fareCop: Int,
    val pickupKm: Double,
    val pickupMin: Int,
    val tripKm: Double,
    val tripMin: Int,
    val rating: Double? = null,
    val passengerTrips: Int? = null,
    val uberDisplayedCopPerKm: Double? = null,
    val confidence: Int = 0
)

data class OfferObservation(
    val fareCop: Int? = null,
    val pickupKm: Double? = null,
    val pickupMin: Int? = null,
    val tripKm: Double? = null,
    val tripMin: Int? = null,
    val rating: Double? = null,
    val passengerTrips: Int? = null,
    val uberDisplayedCopPerKm: Double? = null
) {
    fun isEmpty(): Boolean =
        fareCop == null && pickupKm == null && pickupMin == null &&
        tripKm == null && tripMin == null && rating == null &&
        passengerTrips == null && uberDisplayedCopPerKm == null
}

object OfferParser {
    private val fareLine = Regex("""(?i)^\s*(?:COP\s*\$?\s*|\$\s*)([0-9][0-9.,\s]{2,})\s*$""")
    private val displayedPerKm = Regex("""(?i)\bCOP\s*([0-9][0-9.,]*)\s*/\s*km\b""")
    private val pickup = Regex("""(?i)\bA\s*([0-9]{1,3})\s*min(?:utos?)?\s*\(\s*([0-9]+(?:[.,][0-9]+)?)\s*km\s*\)""")
    private val trip = Regex("""(?i)\bViaje\s*:\s*([0-9]{1,3})\s*min(?:utos?)?\s*\(\s*([0-9]+(?:[.,][0-9]+)?)\s*km\s*\)""")
    private val ratingWithCount = Regex("""(?<![0-9])([1-5](?:[.,][0-9]{1,2}))\s*\(\s*([0-9]{1,6})\s*\)""")
    private val ratingStandalone = Regex("""(?<![0-9.,])([1-5][.,][0-9]{2})(?![0-9]|\s*(?:km|/km))""", RegexOption.IGNORE_CASE)

    fun parse(text: String): OfferCandidate? {
        val observation = observe(text) ?: return null
        return observationToCandidate(observation, 86)
    }

    fun observe(text: String): OfferObservation? {
        val normalized = text.replace('\u00A0', ' ')
        val flat = normalized.replace('\n', ' ')

        val fareCop = normalized.lineSequence()
            .map { it.trim() }
            .mapNotNull { fareLine.matchEntire(it)?.groupValues?.get(1) }
            .mapNotNull(::parseCop)
            .firstOrNull { it in 1000..500000 }

        val p = pickup.find(flat)
        val pickupMin = p?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..120 }
        val pickupKm = p?.groupValues?.getOrNull(2)?.let(::parseDecimal)?.takeIf { it in 0.0..60.0 }

        val t = trip.find(flat)
        val tripMin = t?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..240 }
        val tripKm = t?.groupValues?.getOrNull(2)?.let(::parseDecimal)?.takeIf { it in 0.1..200.0 }

        val shown = displayedPerKm.find(flat)?.groupValues?.getOrNull(1)?.let(::parseMoneyNumber)

        val rc = ratingWithCount.find(flat)
        val rating = rc?.groupValues?.getOrNull(1)?.let(::parseDecimal)
            ?: ratingStandalone.findAll(flat)
                .mapNotNull { it.groupValues.getOrNull(1)?.let(::parseDecimal) }
                .firstOrNull { it in 1.0..5.0 }

        val trips = rc?.groupValues?.getOrNull(2)?.toIntOrNull()

        val o = OfferObservation(
            fareCop, pickupKm, pickupMin, tripKm, tripMin,
            rating?.takeIf { it in 1.0..5.0 }, trips, shown
        )
        return o.takeUnless { it.isEmpty() }
    }

    internal fun observationToCandidate(o: OfferObservation, baseConfidence: Int): OfferCandidate? {
        val fare = o.fareCop ?: return null
        val pk = o.pickupKm ?: return null
        val pm = o.pickupMin ?: return null
        val tk = o.tripKm ?: return null
        val tm = o.tripMin ?: return null

        if (fare !in 1000..500000 || pk !in 0.0..60.0 || tk !in 0.1..200.0) return null
        if (pm !in 0..120 || tm !in 1..240) return null

        val totalKm = pk + tk
        if (totalKm <= 0.0) return null

        val computed = fare / totalKm
        val relativeError = o.uberDisplayedCopPerKm?.let { kotlin.math.abs(it - computed) / computed }
        if (relativeError != null && relativeError > 0.12) return null

        var confidence = baseConfidence
        if (o.rating != null) confidence += 3
        if (o.uberDisplayedCopPerKm != null) confidence += 3
        if (relativeError != null && relativeError <= 0.08) confidence += 6

        return OfferCandidate(
            fare, pk, pm, tk, tm, o.rating, o.passengerTrips,
            o.uberDisplayedCopPerKm, confidence.coerceAtMost(98)
        )
    }

    private fun parseCop(raw: String): Int? = raw.filter(Char::isDigit).toIntOrNull()

    private fun parseDecimal(raw: String): Double? {
        var x = raw.trim().replace(" ", "")
        x = when {
            ',' in x && '.' in x -> x.replace(",", "")
            ',' in x -> {
                val tail = x.substringAfterLast(',')
                if (tail.length == 3) x.replace(",", "") else x.replace(",", ".")
            }
            else -> x
        }
        return x.toDoubleOrNull()
    }

    private fun parseMoneyNumber(raw: String): Double? {
        var x = raw.trim().replace(" ", "")
        x = when {
            ',' in x && '.' in x -> {
                if (x.lastIndexOf(',') > x.lastIndexOf('.'))
                    x.replace(".", "").replace(",", ".")
                else x.replace(",", "")
            }
            ',' in x -> if (x.substringAfterLast(',').length == 3) x.replace(",", "") else x.replace(",", ".")
            '.' in x -> if (x.substringAfterLast('.').length == 3) x.replace(".", "") else x
            else -> x
        }
        return x.toDoubleOrNull()
    }
}
