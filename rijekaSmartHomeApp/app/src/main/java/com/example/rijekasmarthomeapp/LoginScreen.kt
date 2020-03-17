package com.example.rijekasmarthomeapp

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.*
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    //val url = "http://rijekastan.ddns.net:80/"
    //var url = "https://www.google.com/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val url: String = getString(R.string.mainUrl)

/*        loginButton.setOnClickListener({
            CoroutineScope(IO).launch {
                if(usernameText.text == username && passwordText.text == password) {

                }
            }
        })*/

        loginButton.setOnClickListener {
            CoroutineScope(IO).launch {

                //val jsonString = makeHttpRequest(url_rl)
                try {
                    val username: String = usernameText.text.toString()
                    val password: String = passwordText.text.toString()

                    login(url, username, password)
                    //makeHttpRequest(url)
                    //val login = POST_req(url, loginVal, 10000)
                    //val pageContent = POST_req(url_rl, "", 10000)
                    //println(pageContent)
                    /*val json = JSONObject(jsonString)
                        val water_heater = json.getString("water_heater")
                        println(water_heater)
                        if (water_heater == "0") {
                            toggleButton.setText("ON")
                        }
                        else {
                            toggleButton.setText("OFF")
                        }*/
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun login(url: String, username: String, password: String) {
        val getLoginString: String = url + "login?username=" + username + "&password=" + password

        // Try to login
        val loginForm: Connection.Response = Jsoup.connect(url + getLoginString)
            .followRedirects(true)
            .method(Connection.Method.GET)
            .timeout(10000)
            .header("Connection", "close")
            .execute()

        val loginCookies: Map<String, String> = loginForm.cookies()

        // Start the other activity if the login is successful
        if (loginCookies.isNotEmpty()) {
            startActivity(
                Intent(this, MainScreen::class.java)
                    .putExtra("Map", loginCookies as Serializable)
            )
        }

        // Show (and create) message if the login isn't successful
        else {
            val loginLayout: LinearLayout = findViewById(R.id.loginLayout)
            val wrongLoginText = TextView(this)


            wrongLoginText.text = "Wrong username or password, please try again."
            wrongLoginText.setTextColor(
                ContextCompat.getColor(
                    applicationContext,
                    R.color.colorAccent
                )
            )
            wrongLoginText.gravity = Gravity.CENTER

            // For updating UI elements. Since this in a background function (Coroutine)
            // UI can't be updated just like that
            this.runOnUiThread{
                loginLayout.addView(wrongLoginText)
            }
        }

        usernameText.setText("")
        passwordText.setText("")
        //val waterHeaterSwitcher : Connection.Response = Jsoup.connect(url + "water_heater.html").cookies(loginCookies).execute()
    }
}

