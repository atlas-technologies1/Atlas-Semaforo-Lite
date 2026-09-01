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

object OfferParser {
    private val fareLine = Regex("""(?i)^\s*(?:COP\s*\$?\s*|\$\s*)([0-9][0-9.,\s]{2,})\s*$""")
    private val displayedPerKm = Regex("""(?i)\bCOP\s*([0-9][0-9.,]*)\s*/\s*km\b""")
    private val pickup = Regex("""(?i)\bA\s*([0-9]{1,3})\s*min(?:utos?)?\s*\(\s*([0-9]+(?:[.,][0-9]+)?)\s*km\s*\)""")
    private val trip = Regex("""(?i)\bViaje\s*:\s*([0-9]{1,3})\s*min(?:utos?)?\s*\(\s*([0-9]+(?:[.,][0-9]+)?)\s*km\s*\)""")
    private val ratingWithCount = Regex("""(?<![0-9])([1-5](?:[.,][0-9]{1,2}))\s*\(\s*([0-9]{1,6})\s*\)""")
    private val ratingStandalone = Regex("""(?<![0-9.,])([1-5][.,][0-9]{2})(?![0-9]|\s*(?:km|/km))""", RegexOption.IGNORE_CASE)

    fun parse(text: String): OfferCandidate? {
        val normalized = text.replace('\u00A0', ' ')
        val fareCop = normalized.lineSequence()
            .map { it.trim() }
            .mapNotNull { fareLine.matchEntire(it)?.groupValues?.get(1) }
            .mapNotNull(::parseCop)
            .firstOrNull { it in 1000..500000 }
            ?: return null

        val flat = normalized.replace('\n', ' ')
        val p = pickup.find(flat) ?: return null
        val t = trip.find(flat) ?: return null

        val pickupMin = p.groupValues[1].toIntOrNull() ?: return null
        val pickupKm = parseDecimal(p.groupValues[2]) ?: return null
        val tripMin = t.groupValues[1].toIntOrNull() ?: return null
        val tripKm = parseDecimal(t.groupValues[2]) ?: return null

        if (pickupMin !in 0..120 || tripMin !in 1..240) return null
        if (pickupKm !in 0.0..60.0 || tripKm !in 0.1..200.0) return null

        val shown = displayedPerKm.find(flat)?.groupValues?.get(1)?.let(::parseMoneyNumber)
        val ratingCountMatch = ratingWithCount.find(flat)
        val parsedRating = ratingCountMatch?.groupValues?.get(1)?.let(::parseDecimal)
            ?: ratingStandalone.findAll(flat)
                .mapNotNull { it.groupValues.getOrNull(1)?.let(::parseDecimal) }
                .firstOrNull { it in 1.0..5.0 }
        val passengerTrips = ratingCountMatch?.groupValues?.getOrNull(2)?.toIntOrNull()
        val totalKm = pickupKm + tripKm
        if (totalKm <= 0.0) return null

        val computed = fareCop / totalKm
        val relativeError = shown?.let { kotlin.math.abs(it - computed) / computed }
        if (relativeError != null && relativeError > 0.12) return null

        var confidence = 86
        if (parsedRating != null) confidence += 3
        if (shown != null) confidence += 3
        if (relativeError != null && relativeError <= 0.08) confidence += 6

        return OfferCandidate(
            fareCop, pickupKm, pickupMin, tripKm, tripMin,
            parsedRating, passengerTrips, shown, confidence.coerceAtMost(98)
        )
    }

    private fun parseMoneyNumber(raw: String): Double? {
        var x = raw.trim().replace(" ", "")
        if (x.isEmpty()) return null
        x = when {
            ',' in x && '.' in x -> {
                val lastComma = x.lastIndexOf(',')
                val lastDot = x.lastIndexOf('.')
                if (lastComma > lastDot) x.replace(".", "").replace(",", ".")
                else x.replace(",", "")
            }
            ',' in x -> {
                val tail = x.substringAfterLast(',')
                if (tail.length == 3) x.replace(",", "") else x.replace(",", ".")
            }
            '.' in x -> {
                val tail = x.substringAfterLast('.')
                if (tail.length == 3) x.replace(".", "") else x
            }
            else -> x
        }
        return x.toDoubleOrNull()
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
}
