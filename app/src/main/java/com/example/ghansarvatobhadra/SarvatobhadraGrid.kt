package com.example.ghansarvatobhadra

data class SbcCell(val label: String, val type: String)

object SarvatobhadraGrid {
    val cells: Array<Array<SbcCell>> = run {
        val a = Array(9) { Array(9) { SbcCell("", "empty") } }
        for (r in 0..8) for (c in 0..8)
            a[r][c] = SbcCell(SarvatobhadraEngine.labels[r][c], "cell")
        a
    }

    fun cellForNakshatra(index: Int): Pair<Int, Int>? =
        SarvatobhadraEngine.coordinateForNakshatra(index)
}
