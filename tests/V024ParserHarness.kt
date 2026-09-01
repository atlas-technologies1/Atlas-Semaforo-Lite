package com.atlas.semaforo

private fun p(name:String, ok:Boolean){ if(!ok) error("FAIL: $name"); println("$name: PASS") }
fun main(){
    val text = """
        COP 25.045
        COP 2.582 /km
        4.93 (193)
        A 6 min (1,0 km)
        Viaje: 29 min (8,7 km)
    """.trimIndent()
    val o=OfferParser.parse(text)
    p("PhysicalGreenFixtureParsed", o != null)
    p("PassengerTrips193Parsed", o?.passengerTrips == 193)
    p("Rating493Parsed", o?.rating == 4.93)
    p("Confidence98", o?.confidence == 98)
}
