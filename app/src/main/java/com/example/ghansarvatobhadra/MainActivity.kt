package com.example.ghansarvatobhadra

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    override fun onCreate(b: Bundle?) { super.onCreate(b); ui() }

    private fun ui() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 10, 10, 20)
        }
        val scroll = ScrollView(this); scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "घन सर्वतोभद्र चक्र • v0.6"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(80, 25, 100))
            setPadding(0, 8, 0, 12)
        })

        fun e(h: String) = EditText(this).apply { hint = h; setSingleLine(true) }

        val date = e("Birth Date  DD-MM-YYYY")
        val time = e("Birth Time  HH:MM")
        val name = e("Name / Naam Akshara")

        // Place autocomplete field
        val placeInput = AutoCompleteTextView(this).apply {
            hint = "Place (type 3+ letters)"
            setSingleLine(true)
            threshold = 3
        }

        val lat = e("Latitude (auto-filled)")
        val lon = e("Longitude (auto-filled)")

        listOf(date, time, placeInput, lat, lon, name).forEach(root::addView)

        // === Place Autocomplete Logic ===
        var searchJob: Job? = null
        var currentResults: List<PlaceResult> = emptyList()
        placeInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim() ?: return
                if (q.length < 3) return
                searchJob?.cancel()
                searchJob = scope.launch {
                    delay(400)
                    val results = PlaceAutocomplete.search(q)
                    currentResults = results
                    if (results.isNotEmpty()) {
                        val names = results.map {
                            "${it.shortName} (${"%.2f".format(it.latitude)}, ${"%.2f".format(it.longitude)})"
                        }
                        placeInput.setAdapter(ArrayAdapter(
                            this@MainActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            names
                        ))
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        placeInput.setOnItemClickListener { _, _, position, _ ->
            val r = currentResults.getOrNull(position) ?: return@setOnItemClickListener
            lat.setText(r.latitude.toString())
            lon.setText(r.longitude.toString())
        }

        val btn = Button(this).apply { text = "Generate Natal + Vedha" }
        root.addView(btn)

        val pdfBtn = Button(this).apply {
            text = "📄 Export PDF"
            isEnabled = false
        }
        root.addView(pdfBtn)

        val grid = GridLayout(this).apply { columnCount = 9; rowCount = 9 }
        val views = Array(9) { arrayOfNulls<TextView>(9) }
        for (r in 0..8) for (c in 0..8) {
            val tv = TextView(this).apply {
                text = SarvatobhadraEngine.labels[r][c]
                gravity = Gravity.CENTER
                textSize = if (text.length > 11) 7.5f else 9.5f
                setPadding(1, 2, 1, 2)
                setBackgroundColor(Color.rgb(250, 247, 252))
            }
            views[r][c] = tv
            grid.addView(tv, GridLayout.LayoutParams().apply {
                width = 0; height = 66
                columnSpec = GridLayout.spec(c, 1, 1f)
                rowSpec = GridLayout.spec(r, 1, 1f)
            })
        }
        root.addView(grid)

        val result = TextView(this).apply {
            textSize = 12f
            setPadding(4, 14, 4, 10)
            setTextColor(Color.rgb(30, 30, 30))
        }
        root.addView(result)

        var lastChart: ChartResponse? = null
        var lastLat = 0.0
        var lastLon = 0.0

        btn.setOnClickListener {
            val base = BuildConfig.EPHEMERIS_BASE_URL
            val la = lat.text.toString().toDoubleOrNull()
            val lo = lon.text.toString().toDoubleOrNull()

            if (base.isBlank()) {
                result.text = "❌ Server URL set nahi hai.\nGitHub Secrets mein EPHEMERIS_BASE_URL daalein."
                return@setOnClickListener
            }
            if (la == null || lo == null) {
                result.text = "❌ Place select karein ya Latitude/Longitude bharein."
                return@setOnClickListener
            }

            btn.isEnabled = false
            pdfBtn.isEnabled = false
            result.text = "⏳ Real planetary calculation chal rahi hai…\n(Pehli baar 30-60 sec lag sakte hain)"

            scope.launch {
                val isoDt = parseDateTimeToIso(date.text.toString(), time.text.toString())
                val out = EphemerisApi(base).chart(isoDt, "Asia/Kolkata", la, lo)
                btn.isEnabled = true
                result.text = out.fold({ ch ->
                    lastChart = ch
                    lastLat = la
                    lastLon = lo
                    pdfBtn.isEnabled = true

                    val moon = ch.bodies.firstOrNull { it.planet.equals("moon", true) }
                    val hits = SarvatobhadraEngine.hits(ch.bodies)
                    val natal = SarvatobhadraEngine.natalPoints(
                        moon, ch.lagna, name.text.toString(), ch.tithi, ch.weekday
                    )
                    for (r in 0..8) for (c in 0..8)
                        views[r][c]?.setBackgroundColor(Color.rgb(250, 247, 252))
                    natal.forEach { views[it.row][it.col]?.setBackgroundColor(Color.rgb(255, 235, 120)) }

                    val sb = StringBuilder()
                    sb.append("✅ Real Swiss Ephemeris Data\n\n")
                    sb.append("Tithi: ${ch.tithi}   Weekday: ${ch.weekday}\n")
                    ch.lagna?.let {
                        sb.append("Lagna: ${AstrologyConstants.rashis[it.rashi - 1]} (${"%.2f".format(it.degreeInRashi)}°)\n")
                    }
                    sb.append("\n── ग्रह स्थिति ──\n")
                    ch.bodies.forEach { p ->
                        val nak = AstrologyConstants.nakshatras[p.nakshatraIndex]
                        val rash = AstrologyConstants.rashis[p.rashi - 1]
                        sb.append("${p.planet.uppercase()}: ${"%.2f".format(p.longitude)}° → $rash, $nak P${p.pada}")
                        if (p.retrograde) sb.append(" ℞")
                        sb.append("\n")
                    }
                    sb.append("\n── Natal Points ──\n")
                    natal.forEach { sb.append("• ${it.label} → ${SarvatobhadraEngine.labels[it.row][it.col]}\n") }
                    sb.append("\n── Vedha Hits ──\n")
                    hits.take(60).forEach { sb.append("• ${it.planet} [${it.direction}] → ${it.targetLabel} (${it.nature})\n") }
                    sb.toString()
                }, { e -> "❌ Error: ${e.message}" })
            }
        }

        pdfBtn.setOnClickListener {
            val ch = lastChart ?: return@setOnClickListener
            try {
                val file = PdfExporter.export(
                    this, name.text.toString(),
                    date.text.toString(), time.text.toString(),
                    placeInput.text.toString(),
                    lastLat, lastLon, ch
                )
                Toast.makeText(this,
                    "✅ PDF saved:\n${file.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this, "❌ PDF error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        setContentView(scroll)
    }

    private fun parseDateTimeToIso(date: String, time: String): String {
        return try {
            val (d, m, y) = date.split("-")
            val t = if (time.length == 5) "$time:00" else time
            "${y}-${m.padStart(2, '0')}-${d.padStart(2, '0')}T$t"
        } catch (ex: Exception) {
            "${date}T${time}"
        }
    }
}
