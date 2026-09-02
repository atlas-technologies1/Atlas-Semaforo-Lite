package com.atlas.semaforo

class IncrementalOfferAssembler(
    private val mergeWindowMs: Long = 1800L,
    private val fareToleranceCop: Int = 150
) {
    private var lastAtMs = 0L
    private var state = OfferObservation()

    fun ingest(nowMs: Long, observation: OfferObservation?): OfferCandidate? {
        if (observation == null || observation.isEmpty()) {
            expireIfNeeded(nowMs)
            return null
        }

        val expired = lastAtMs != 0L && nowMs - lastAtMs > mergeWindowMs
        val oldFare = state.fareCop
        val newFare = observation.fareCop
        val fareChanged = oldFare != null && newFare != null &&
            kotlin.math.abs(oldFare - newFare) > fareToleranceCop

        if (expired || fareChanged) reset()
        lastAtMs = nowMs

        state = OfferObservation(
            fareCop = observation.fareCop ?: state.fareCop,
            pickupKm = observation.pickupKm ?: state.pickupKm,
            pickupMin = observation.pickupMin ?: state.pickupMin,
            tripKm = observation.tripKm ?: state.tripKm,
            tripMin = observation.tripMin ?: state.tripMin,
            rating = observation.rating ?: state.rating,
            passengerTrips = observation.passengerTrips ?: state.passengerTrips,
            uberDisplayedCopPerKm = observation.uberDisplayedCopPerKm ?: state.uberDisplayedCopPerKm
        )

        return OfferParser.observationToCandidate(state, 90)
    }

    fun onNoOffer(nowMs: Long) = expireIfNeeded(nowMs)
    fun clear() = reset()

    private fun expireIfNeeded(nowMs: Long) {
        if (lastAtMs != 0L && nowMs - lastAtMs > mergeWindowMs) reset()
    }

    private fun reset() {
        lastAtMs = 0L
        state = OfferObservation()
    }
}
