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

        val btn = Button(this).apply { text = "Generate Natal +
