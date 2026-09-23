package com.example.ghansarvatobhadra

data class NatalPoint(val label: String, val row: Int, val col: Int)

data class VedhaHit(
    val planet: String,
    val motion: String,
    val direction: String,
    val targetLabel: String,
    val row: Int,
    val col: Int,
    val nature: String
)

object SarvatobhadraEngine {

    val labels = arrayOf(
        arrayOf("ई","Dhanishtha","Shatabhisha","Purva Bhadrapada","Uttara Bhadrapada","Revati","Ashwini","Bharani","अ"),
        arrayOf("Shravana","ऋ","ग","स","द","च","ल","उ","Krittika"),
        arrayOf("Abhijit","ख","ऐ","Aquarius","Pisces","Aries","लृ","अ","Rohini"),
        arrayOf("Uttara Ashadha","ज","Capricorn","अः","Rikta / Friday","ओ","Taurus","व","Mrigashira"),
        arrayOf("Purva Ashadha","भ","Sagittarius","Jaya / Thursday","Poorna / Saturday","Nanda / Sun-Tue","Gemini","क","Ardra"),
        arrayOf("Mula","य","Scorpio","अं","Bhadra / Mon-Wed","औ","Cancer","ह","Punarvasu"),
        arrayOf("Jyeshtha","न","ए","Libra","Virgo","Leo","लू","ड","Pushya"),
        arrayOf("Anuradha","ऋ","त","र","प","ट","म","ऊ","Ashlesha"),
        arrayOf("इ","Vishakha","Swati","Chitra","Hasta","Uttara Phalguni","Purva Phalguni","Magha","आ")
    )

    private val aliases = mapOf(
        "Mrigasira" to "Mrigashira", "Asresha" to "Ashlesha", "Moola" to "Mula",
        "Jyestha" to "Jyeshtha", "Uttarashada" to "Uttara Ashadha",
        "Poorvashada" to "Purva Ashadha", "Dhanista" to "Dhanishtha",
        "Satabhisa" to "Shatabhisha", "Poorvabhadrapada" to "Purva Bhadrapada",
        "Uttarabhadrapada" to "Uttara Bhadrapada", "Poorva Phalguni" to "Purva Phalguni"
    )

    private val nakCoords: Map<String, Pair<Int, Int>> = buildMap {
        for (r in 0..8) for (c in 0..8) {
            val x = labels[r][c]
            val canonical = aliases[x] ?: x
            if (AstrologyConstants.nakshatras.contains(canonical)) put(canonical, r to c)
        }
    }

    fun coordinateForNakshatra(index: Int) =
        nakCoords[AstrologyConstants.nakshatras[index.coerceIn(0, 26)]]

    private fun directionFor(row: Int, col: Int): String = when {
        row == 0 -> "down"
        row == 8 -> "up"
        col == 0 -> "right"
        col == 8 -> "left"
        else -> "down"
    }

    private fun step(row: Int, col: Int, dr: Int, dc: Int): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        var r = row + dr; var c = col + dc
        while (r in 0..8 && c in 0..8) { out += r to c; r += dr; c += dc }
        return out
    }

    fun vedhaLines(row: Int, col: Int, motion: String, planet: String):
            Map<String, List<Pair<Int, Int>>> {
        val d = directionFor(row, col)
        val (f, l, r) = when (d) {
            "down"  -> Triple(1 to 0, 1 to -1, 1 to 1)
            "up"    -> Triple(-1 to 0, -1 to 1, -1 to -1)
            "right" -> Triple(0 to 1, -1 to 1, 1 to 1)
            else    -> Triple(0 to -1, 1 to -1, -1 to -1)
        }
        val front = step(row, col, f.first, f.second)
        val left = step(row, col, l.first, l.second)
        val right = step(row, col, r.first, r.second)
        val allThree = planet.lowercase() in setOf("sun", "moon", "rahu", "ketu")
        return if (allThree) mapOf("front" to front, "left" to left, "right" to right)
        else when (motion.lowercase()) {
            "retrograde" -> mapOf("right" to right)
            "fast" -> mapOf("left" to left)
            else -> mapOf("front" to front)
        }
    }

    fun hits(positions: List<PlanetPosition>): List<VedhaHit> {
        val result = mutableListOf<VedhaHit>()
        positions.forEach { p ->
            val coord = coordinateForNakshatra(p.nakshatraIndex) ?: return@forEach
            val motion = when {
                p.retrograde -> "retrograde"
                p.speed > 1.2 -> "fast"
                else -> "normal"
            }
            vedhaLines(coord.first, coord.second, motion, p.planet).forEach { (dir, cells) ->
                cells.forEach { (r, c) ->
                    val label = labels[r][c]
                    if (label.isNotBlank()) {
                        val nature = when (p.planet.lowercase()) {
                            in AstrologyConstants.benefics -> "benefic"
                            in AstrologyConstants.malefics -> "malefic"
                            else -> "neutral"
                        }
                        result += VedhaHit(p.planet, motion, dir, label, r, c, nature)
                    }
                }
            }
        }
        return result
    }

    fun natalPoints(
        moon: PlanetPosition?, lagna: PlanetPosition?,
        name: String, tithi: Int, weekday: Int
    ): List<NatalPoint> {
        val out = mutableListOf<NatalPoint>()
        moon?.let { coordinateForNakshatra(it.nakshatraIndex)?.let { (r, c) -> out += NatalPoint("Janma Nakshatra", r, c) } }
        lagna?.let { rashiCoord(it.rashi)?.let { (r, c) -> out += NatalPoint("Lagna Rashi", r, c) } }
        name.trim().firstOrNull()?.let { ch ->
            findNameAkshara(ch)?.let { (r, c) -> out += NatalPoint("Naam Akshara", r, c) }
        }
        tithiGroupCoord(tithi)?.let { (r, c) -> out += NatalPoint("Janma Tithi", r, c) }
        weekdayCoord(weekday)?.let { (r, c) -> out += NatalPoint("Janma Vara", r, c) }
        return out.distinctBy { it.row to it.col }
    }

    private fun rashiCoord(rashi: Int): Pair<Int, Int>? {
        val wanted = AstrologyConstants.rashis[(rashi - 1).coerceIn(0, 11)]
        val map = mapOf(
            "Mesha" to "Aries", "Vrishabha" to "Taurus", "Mithuna" to "Gemini",
            "Karka" to "Cancer", "Simha" to "Leo", "Kanya" to "Virgo",
            "Tula" to "Libra", "Vrishchika" to "Scorpio", "Dhanu" to "Sagittarius",
            "Makara" to "Capricorn", "Kumbha" to "Aquarius", "Meena" to "Pisces"
        )
        for (r in 0..8) for (c in 0..8) if (labels[r][c] == map[wanted]) return r to c
        return null
    }

    private fun tithiGroupCoord(tithi: Int): Pair<Int, Int>? = when ((tithi - 1) % 15 + 1) {
        1, 6, 11 -> 4 to 5
        2, 7, 12 -> 5 to 4
        3, 8, 13 -> 4 to 3
        4, 9, 14 -> 3 to 4
        else -> 4 to 4
    }

    private fun weekdayCoord(day: Int): Pair<Int, Int>? = when (day) {
        0, 2 -> 4 to 5
        1, 3 -> 5 to 4
        4 -> 4 to 3
        5 -> 3 to 4
        6 -> 4 to 4
        else -> null
    }

    private fun findNameAkshara(ch: Char): Pair<Int, Int>? {
        val s = ch.lowercaseChar()
        val keys = mapOf(
            'a' to "अ",'i' to "इ",'u' to "उ",'e' to "ए",'o' to "ओ",
            'k' to "क",'g' to "ग",'c' to "च",'j' to "ज",'t' to "त",
            'd' to "द",'n' to "न",'p' to "प",'b' to "भ",'m' to "म",
            'y' to "य",'r' to "र",'l' to "ल",'v' to "व",'s' to "स",'h' to "ह"
        )
        val target = keys[s] ?: return null
        for (r in 0..8) for (c in 0..8) if (labels[r][c] == target) return r to c
        return null
    }
}
