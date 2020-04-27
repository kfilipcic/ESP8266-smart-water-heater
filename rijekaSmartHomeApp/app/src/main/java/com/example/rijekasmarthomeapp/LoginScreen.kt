package com.example.rijekasmarthomeapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
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
        var usernameString: String
        var passwordString: String

        val usernameEditText: EditText = findViewById(R.id.usernameText)
        val passwordEditText: EditText = findViewById(R.id.passwordText)

        //REMOVE when done debugging
        usernameEditText.setText("rijekastan")
        passwordEditText.setText("A3C01610A252")

        val saveLoginCheckbox: CheckBox = findViewById(R.id.loginCheckbox)

        val loginButton: Button = findViewById(R.id.loginButton)

        val loginPreferences = getSharedPreferences("saveLogin", Context.MODE_PRIVATE)
        val loginPrefsEditor = loginPreferences.edit()

        val saveLogin = loginPreferences.getBoolean("saveLogin", false)

        if (saveLogin) {
            usernameEditText.setText(loginPreferences.getString("username", ""))
            usernameEditText.setText(loginPreferences.getString("password", ""))
            saveLoginCheckbox.isChecked = true
        }

        loginButton.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(usernameEditText.getWindowToken(), 0)

            usernameString = usernameEditText.text.toString()
            passwordString = passwordEditText.text.toString()

            if (saveLoginCheckbox.isChecked) {
                loginPrefsEditor.putBoolean("saveLogin", true)
                loginPrefsEditor.putString("username", usernameString)
                loginPrefsEditor.putString("password", passwordString)
            } else {
                loginPrefsEditor.clear()
                loginPrefsEditor.apply()
            }
        }

            loginButton.setOnClickListener {
                CoroutineScope(IO).launch {

                    //val jsonString = makeHttpRequest(url_rl)
                    try {
                        val username: String = usernameText.text.toString()
                        val password: String = passwordText.text.toString()

                        login(url, username, password)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        @SuppressLint("SetTextI18n")
        private fun login(url: String, username: String, password: String) {
            val getLoginString: String =
                url + "login?username=" + username + "&password=" + password

            // Try to login
            System.out.println("Login url: " + getLoginString)
            val loginForm: Connection.Response = Jsoup.connect( getLoginString)
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
                finish()
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
                this.runOnUiThread {
                    loginLayout.addView(wrongLoginText)
                }
            }

        }
    }

