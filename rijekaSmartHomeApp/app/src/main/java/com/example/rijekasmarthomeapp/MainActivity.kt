package com.example.rijekasmarthomeapp
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class MainActivity : AppCompatActivity() {

    //val url = "http://rijekastan.ddns.net:80/"
    val url = "http://paup.hopto.org/"
    //var url = "https://www.google.com/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toggleButton.setOnClickListener({
            val url_rl = url + "water_heater.htm"
            val loginVal = "username=rijekastan&password=A3C01610A252"
            CoroutineScope(IO).launch {

                //val jsonString = makeHttpRequest(url_rl)
                try {
                    makeHttpRequest(url)
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
                }
                catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        )
    }

    private suspend fun makeHttpRequest(urlStr: String){
        val url: URL
        var urlConnection: HttpURLConnection? = null
        try {
            url = URL(urlStr)

            urlConnection = url
                .openConnection() as HttpURLConnection
            /*
            var okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor(object : Interceptor {
                    @Throws(IOException::class)
                    override fun intercept(chain: Interceptor.Chain): Response<*> {
                        val request =
                            chain.request().newBuilder().addHeader("Connection", "close").build()
                        return chain.proceed(request)
                    }
                })
                .build()*/

            val `in` = urlConnection.inputStream

            val isw = InputStreamReader(`in`)

            var data = isw.read()
            while (data != -1) {
                val current = data.toChar()
                data = isw.read()
                print(current)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            urlConnection?.disconnect()
        }
    }

    private fun getData(conn: HttpURLConnection): String {
        var text: StringBuffer = StringBuffer()
        var inp: InputStreamReader = InputStreamReader(conn.getContent() as InputStream)
        var buff: BufferedReader = BufferedReader(inp)
        var line: String
        do {
            line = buff.readLine()
            text.append(line + "\n")
        } while (line != null)
        return text.toString()
    }

    //Methods for sending requests and saving cookie:
//(this no needs for changing, can only past to you project)
    fun POST_req(url: String, post_data: String, len: Int): String {
        var addr: URL? = null
        try {
            addr = URL(url)
        } catch (e: MalformedURLException) {
            return "Некорректный URL"
        }

        val data = StringBuffer()
        var conn: HttpURLConnection? = null
        try {
            conn = addr.openConnection() as HttpURLConnection
        } catch (e: IOException) {
            return "Open connection error"
        }

        conn.setRequestProperty("Connection", "keep-alive")
        conn.setRequestProperty("Accept-Language", "ru,en-GB;q=0.8,en;q=0.6")
        conn.setRequestProperty("Accept-Charset", "utf-8")
        conn.setRequestProperty(
            "Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
        )
        conn.setRequestProperty("Cookie", "")
        conn.doOutput = true
        conn.doInput = true
        //conn.setInstanceFollowRedirects(true);
        set_cookie(conn)

        //POST data:
        data.append(post_data)
        try {
            conn.connect()
        } catch (e: IOException) {
            return "Connecting error"
        }

        var dataOS: DataOutputStream? = null
        try {
            dataOS = DataOutputStream(conn.outputStream)
        } catch (e2: IOException) {
            return "Out stream error"
        }

        try {
            (dataOS as DataOutputStream).writeBytes(data.toString())
        } catch (e: IOException) {
            return "Out stream error 1"
        }

        /*If redirect: */
        val status: Int
        try {
            status = conn.responseCode
        } catch (e2: IOException) {
            return "Response error"
        }

        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
            val new_url = conn.getHeaderField("Location")
            val cookies = conn.getHeaderField("Set-Cookie")
            val red_url: URL
            try {
                red_url = URL(new_url)
            } catch (e: MalformedURLException) {
                return "Redirect error"
            }

            try {
                conn = red_url.openConnection() as HttpURLConnection
            } catch (e: IOException) {
                return "Redirect connection error"
            }

            //conn.setRequestProperty("Content-type", "text/html");
            conn.setRequestProperty("Connection", "keep-alive")
            conn.setRequestProperty("Accept-Language", "ru,en-GB;q=0.8,en;q=0.6")
            conn.setRequestProperty("Accept-Charset", "utf-8")
            conn.setRequestProperty(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
            )
            conn.setRequestProperty("Cookie", cookies)
            conn.doOutput = true
            conn.doInput = true
            //conn.setInstanceFollowRedirects(true);
        }

        var `in`: InputStream? = null
        try {
            `in` = conn.inputStream as InputStream
        } catch (e: IOException) {
            return "In stream error"
        }

        var reader: InputStreamReader? = null
        try {
            reader = InputStreamReader(`in`, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            return "In stream error"
        }

        val buf = CharArray(len)
        try {
            reader!!.read(buf)
        } catch (e: IOException) {
            return "In stream error"
        }

        get_cookie(conn)

        return String(buf)
    }

    fun get_cookie(conn: HttpURLConnection) {
        val sh_pref_cookie = getSharedPreferences("cookies", Context.MODE_PRIVATE)
        val cook_new: String
        val COOKIES_HEADER: String
        if (conn.getHeaderField("Set-Cookie") != null) {
            COOKIES_HEADER = "Set-Cookie"
        } else {
            COOKIES_HEADER = "Cookie"
        }
        cook_new = conn.getHeaderField(COOKIES_HEADER)
        if (cook_new.indexOf("sid", 0) >= 0) {
            val editor = sh_pref_cookie.edit()
            editor.putString("Cookie", cook_new)
            editor.commit()
        }
    }

    fun set_cookie(conn: HttpURLConnection) {
        val sh_pref_cookie = getSharedPreferences("cookies", Context.MODE_PRIVATE)
        val COOKIES_HEADER = "Cookie"
        val cook = sh_pref_cookie.getString(COOKIES_HEADER, "no_cookie")
        if (cook != "no_cookie") {
            conn.setRequestProperty(COOKIES_HEADER, cook)
        }
    }

}

private fun String.replace(s: String, c: Char) {

}


