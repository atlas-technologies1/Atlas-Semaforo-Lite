package com.atlas.semaforo

private fun check24(name: String, ok: Boolean) { if (!ok) error("FAIL: $name"); println("$name: PASS") }

fun main() {
    val o = OfferCandidate(25045, 1.0, 6, 8.7, 29, 4.93, 193, 2582.0, 98)
    val d = SemaforoEngine().evaluate(o)
    val tracker = ServiceMemoryTracker(7000, 2, 2)

    check24("OfferDoesNotBecomeAcceptedImmediately", !tracker.onFrame(1000, o, d, ScreenSignal.OFFER).becameAccepted)
    check24("OneActiveReadInsufficient", !tracker.onFrame(1350, null, null, ScreenSignal.ACTIVE_SERVICE).becameAccepted)
    val accepted = tracker.onFrame(1700, null, null, ScreenSignal.ACTIVE_SERVICE)
    check24("TwoActiveReadsConfirmAcceptance", accepted.becameAccepted)
    check24("AcceptedFareRetained", accepted.activeService?.fareCop == 25045)
    check24("AcceptedRatingRetained", accepted.activeService?.rating == 4.93)
    check24("AcceptedPassengerTripsRetained", accepted.activeService?.passengerTrips == 193)

    val next = OfferCandidate(12000, .5, 3, 4.0, 12, 4.88, 420, 2667.0, 98)
    val nextD = SemaforoEngine().evaluate(next)
    val whileActive = tracker.onFrame(2100, next, nextD, ScreenSignal.OFFER)
    check24("NewOfferDoesNotEraseCurrentService", whileActive.activeService?.fareCop == 25045)

    check24("OneFinishReadInsufficient", !tracker.onFrame(2500, null, null, ScreenSignal.FINISHED_SERVICE).becameFinished)
    val finished = tracker.onFrame(2850, null, null, ScreenSignal.FINISHED_SERVICE)
    check24("TwoFinishReadsClearCurrentService", finished.becameFinished && finished.activeService == null)

    val stale = ServiceMemoryTracker(7000, 2, 2)
    stale.onFrame(1000, o, d, ScreenSignal.OFFER)
    stale.onFrame(9000, null, null, ScreenSignal.ACTIVE_SERVICE)
    val late = stale.onFrame(9350, null, null, ScreenSignal.ACTIVE_SERVICE)
    check24("LateActiveScreenDoesNotMisattributeOldOffer", !late.becameAccepted)

    check24("ClassifierRequiresTwoActiveSignals",
        UberScreenSignalClassifier.classify("Navegar", false) == ScreenSignal.OTHER)
    check24("ClassifierRecognizesActive",
        UberScreenSignalClassifier.classify("Navegar\nRecoger a Juan", false) == ScreenSignal.ACTIVE_SERVICE)
    check24("ClassifierRecognizesFinish",
        UberScreenSignalClassifier.classify("Viaje completado", false) == ScreenSignal.FINISHED_SERVICE)
}
