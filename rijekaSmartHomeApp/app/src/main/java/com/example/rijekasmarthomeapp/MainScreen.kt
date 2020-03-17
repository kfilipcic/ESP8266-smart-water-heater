package com.example.rijekasmarthomeapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main_screen.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.CookieHandler
import java.net.CookieManager

class MainScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        val waterHeaterCheckUrl: String = "water_heater_check.html"
        val waterHeaterSwitchUrl: String = "water_heater_switch.html"
        val updateTimeUrl: String = "updateTime.html"
        val timeDateUrl: String = "entry.html"

        val url: String = getString(R.string.mainUrl)
        val cookies: Map<String, String> =
            this.intent.getSerializableExtra("Map") as Map<String, String>

        val toggleWaterHeaterButton: ImageButton = findViewById(R.id.toggleWaterHeaterButton)
        val timeText: TextView = findViewById(R.id.timeText)

        fun timeDate() {
            CoroutineScope(IO).launch {
               val timeDatePage: Connection.Response = Jsoup.connect(url + timeDateUrl)
                   .cookies(cookies)
                   .execute()

                val doc : Document = timeDatePage.parse()

                val timeDateElements :Elements = doc.select("h1")

               //val timeDateString : Array<String> = emptyArray()

                timeDateElements.forEach { element :Element ->
                //    timeDateString.add(element.text())
                }
            }
        }

        fun waterHeater(waterHeaterUrl:String) {
            CoroutineScope(IO).launch {
                val waterHeaterPage: Connection.Response = Jsoup.connect(url + waterHeaterUrl)
                    .cookies(cookies)
                    .execute()

                val waterHeater = JSONObject(waterHeaterPage.body())
                val waterHeaterState = waterHeater.get("water_heater").toString()

                if (waterHeaterState == "1") {
                    toggleWaterHeaterButton.setImageResource(R.drawable.boiler_off)
                } else {
                    toggleWaterHeaterButton.setImageResource(R.drawable.boiler_on)
                }
            }
        }

        timeDate()
        // Upon starting the activity, check the water heater
        // state for the appropriate selection of the button image
        waterHeater(waterHeaterCheckUrl)

        toggleWaterHeaterButton.setOnClickListener {
            waterHeater(waterHeaterSwitchUrl)
        }
    }
}
