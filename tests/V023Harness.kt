package com.atlas.semaforo

private fun pass(name: String, condition: Boolean) {
    if (!condition) error("FAIL: $name")
    println("$name: PASS")
}

fun main() {
    val defaults = SemaforoPolicy()
    pass("DefaultPolicyValid", defaults.isValid())
    pass("InvalidExcellentBelowMinimumRejected", !defaults.copy(excellentCopPerKm = 1400.0).isValid())
    pass("InvalidRatingRangeRejected", !defaults.copy(minimumRating = 5.1).isValid())

    val greenOffer = OfferCandidate(25045, 1.0, 6, 8.7, 29, 4.93, null, 2582.0, 98)
    pass("DefaultPhysicalGreenRemainsGreen", SemaforoEngine(defaults).evaluate(greenOffer).band == SemaforoBand.GREEN)

    val lowRatingGreenEconomics = greenOffer.copy(rating = 4.50)
    pass("LowRatingCapsToRed", SemaforoEngine(defaults).evaluate(lowRatingGreenEconomics).band == SemaforoBand.RED)

    val mediumRatingGreenEconomics = greenOffer.copy(rating = 4.70)
    pass("MediumRatingCapsGreenToYellow", SemaforoEngine(defaults).evaluate(mediumRatingGreenEconomics).band == SemaforoBand.YELLOW)

    val ratingIgnored = defaults.copy(ratingEnabled = false)
    pass("RatingCanBeDisabled", SemaforoEngine(ratingIgnored).evaluate(lowRatingGreenEconomics).band == SemaforoBand.GREEN)

    val custom = defaults.copy(
        minimumCopPerKm = 1800.0,
        excellentCopPerKm = 2600.0,
        minimumCopPerHour = 30000.0,
        excellentCopPerHour = 45000.0
    )
    pass("CustomPolicyChangesDecision", SemaforoEngine(custom).evaluate(greenOffer).band == SemaforoBand.YELLOW)
}
