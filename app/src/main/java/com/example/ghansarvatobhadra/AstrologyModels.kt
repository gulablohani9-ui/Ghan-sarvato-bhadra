package com.example.ghansarvatobhadra

data class PlanetPosition(
    val planet: String,
    val longitude: Double,
    val latitude: Double = 0.0,
    val speed: Double = 0.0,
    val retrograde: Boolean = false
) {
    val rashi: Int get() = (longitude / 30.0).toInt() % 12 + 1
    val degreeInRashi: Double get() = longitude % 30.0
    val nakshatraIndex: Int get() = (longitude / (360.0 / 27.0)).toInt().coerceIn(0, 26)
    val pada: Int get() = (((longitude % (360.0 / 27.0)) / (360.0 / 108.0)).toInt() + 1).coerceIn(1, 4)
}

object AstrologyConstants {
    val nakshatras = listOf(
        "Ashwini","Bharani","Krittika","Rohini","Mrigashira","Ardra","Punarvasu","Pushya","Ashlesha",
        "Magha","Purva Phalguni","Uttara Phalguni","Hasta","Chitra","Swati","Vishakha","Anuradha",
        "Jyeshtha","Mula","Purva Ashadha","Uttara Ashadha","Shravana","Dhanishtha",
        "Shatabhisha","Purva Bhadrapada","Uttara Bhadrapada","Revati"
    )
    val rashis = listOf(
        "Mesha","Vrishabha","Mithuna","Karka","Simha","Kanya",
        "Tula","Vrishchika","Dhanu","Makara","Kumbha","Meena"
    )
    val benefics = setOf("moon","mercury","jupiter","venus")
    val malefics = setOf("sun","mars","saturn","rahu","ketu")
}
