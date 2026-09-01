package com.atlas.semaforo

data class ActiveService(
    val fareCop: Int,
    val rating: Double?,
    val passengerTrips: Int?,
    val acceptedAtMs: Long,
    val originalBand: SemaforoBand,
    val originalConfidence: Int
)

enum class ScreenSignal { OFFER, ACTIVE_SERVICE, FINISHED_SERVICE, OTHER }

/**
 * Conservative text-only screen classifier. It never clicks or drives Uber.
 * A state change requires multiple visible text signals so one OCR fragment
 * cannot mark an offer as accepted by itself.
 */
object UberScreenSignalClassifier {
    fun classify(text: String, hasOffer: Boolean): ScreenSignal {
        if (hasOffer) return ScreenSignal.OFFER
        val t = text.lowercase()

        val finishHits = listOf(
            "viaje completado", "viaje finalizado", "califica al pasajero",
            "calificar al pasajero", "resumen del viaje"
        ).count { it in t }
        if (finishHits >= 1) return ScreenSignal.FINISHED_SERVICE

        val activeHits = listOf(
            "recoger a", "iniciar viaje", "inicia el viaje", "desliza para iniciar",
            "llegaste", "navegar", "en camino"
        ).count { it in t }
        return if (activeHits >= 2) ScreenSignal.ACTIVE_SERVICE else ScreenSignal.OTHER
    }
}

data class ServiceMemoryEvent(
    val activeService: ActiveService?,
    val becameAccepted: Boolean = false,
    val becameFinished: Boolean = false
)

/**
 * RAM-only service memory. Structured fields only; no screenshots/OCR text are stored.
 */
class ServiceMemoryTracker(
    private val acceptanceWindowMs: Long = 7000L,
    private val requiredActiveReads: Int = 2,
    private val requiredFinishReads: Int = 2
) {
    private var lastOffer: OfferCandidate? = null
    private var lastDecision: SemaforoDecision? = null
    private var lastOfferAtMs: Long = 0L
    private var activeReads = 0
    private var finishReads = 0
    private var active: ActiveService? = null

    fun onFrame(
        nowMs: Long,
        candidate: OfferCandidate?,
        decision: SemaforoDecision?,
        signal: ScreenSignal
    ): ServiceMemoryEvent {
        if (candidate != null) {
            lastOffer = candidate
            lastDecision = decision ?: lastDecision
            lastOfferAtMs = nowMs
            activeReads = 0
            finishReads = 0
            return ServiceMemoryEvent(active)
        }

        if (active == null && lastOffer != null && nowMs - lastOfferAtMs <= acceptanceWindowMs) {
            if (signal == ScreenSignal.ACTIVE_SERVICE) activeReads++ else activeReads = 0
            if (activeReads >= requiredActiveReads) {
                val offer = lastOffer!!
                val d = lastDecision
                active = ActiveService(
                    fareCop = offer.fareCop,
                    rating = offer.rating,
                    passengerTrips = offer.passengerTrips,
                    acceptedAtMs = nowMs,
                    originalBand = d?.band ?: SemaforoBand.YELLOW,
                    originalConfidence = d?.confidence ?: offer.confidence
                )
                clearPending()
                return ServiceMemoryEvent(active, becameAccepted = true)
            }
        } else if (active == null && lastOfferAtMs != 0L && nowMs - lastOfferAtMs > acceptanceWindowMs) {
            clearPending()
        }

        if (active != null) {
            if (signal == ScreenSignal.FINISHED_SERVICE) finishReads++ else finishReads = 0
            if (finishReads >= requiredFinishReads) {
                active = null
                finishReads = 0
                return ServiceMemoryEvent(null, becameFinished = true)
            }
        }
        return ServiceMemoryEvent(active)
    }

    fun current(): ActiveService? = active

    fun clear() {
        active = null
        clearPending()
        finishReads = 0
    }

    private fun clearPending() {
        lastOffer = null
        lastDecision = null
        lastOfferAtMs = 0L
        activeReads = 0
    }
}
