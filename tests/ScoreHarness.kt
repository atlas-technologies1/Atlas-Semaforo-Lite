package com.atlas.semaforo

private fun offer(fare:Int, km:Double, min:Int, rating:Double=4.90)=OfferCandidate(fare, 0.1, 1, km-0.1, min-1, rating, null, null, 95)
fun main(){
    val e=SemaforoEngine()
    val balanced=e.evaluate(offer(20000,8.0,30)) // 2500/km, 40k/h
    check(balanced.band==SemaforoBand.GREEN && balanced.economicScore>=80)
    println("BalancedStrongGreen: PASS")

    // km weak-ish but hour excellent: should be compensable, not automatic red
    val compensated=e.evaluate(offer(14000,10.0,20)) // 1400/km, 42k/h
    check(compensated.band==SemaforoBand.YELLOW && compensated.reason=="hora compensa km")
    println("StrongHourCompensatesWeakKm: PASS")

    // hour weak-ish but km excellent: likewise compensable
    val compensated2=e.evaluate(offer(12000,4.0,30)) // 3000/km, 24k/h
    check(compensated2.band==SemaforoBand.YELLOW && compensated2.reason=="km compensa hora")
    println("StrongKmCompensatesWeakHour: PASS")

    val bothLow=e.evaluate(offer(10000,8.0,30)) // 1250/km, 20k/h
    check(bothLow.band==SemaforoBand.RED)
    println("BothBelowMinimumRed: PASS")

    val floor=e.evaluate(offer(10000,9.0,12)) // below km floor, great hour
    check(floor.band!=SemaforoBand.GREEN)
    println("HardFloorCapsGreen: PASS")

    val noRating=SemaforoEngine(SemaforoPolicy(ratingEnabled=false)).evaluate(offer(20000,8.0,30,3.0))
    check(noRating.band==SemaforoBand.GREEN)
    println("RatingCanBeDisabled: PASS")
}
