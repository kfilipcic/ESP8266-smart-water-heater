package com.example.rijekasmarthomeapp

import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_device_dialog.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.io.Serializable
import java.lang.Exception
import java.lang.NumberFormatException
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.floor
import kotlin.math.round

class DeviceDialog : AppCompatActivity() {
    private lateinit var devicesListPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var cookiePreferences: SharedPreferences
    private lateinit var cookieEditor: SharedPreferences.Editor

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_dialog)

        devicesListPreferences = applicationContext.getSharedPreferences("devicesList", 0)
        editor = devicesListPreferences.edit()

        cookiePreferences = applicationContext.getSharedPreferences("cookies", 0)
        cookieEditor = cookiePreferences.edit()

        val cookie = cookiePreferences.getString("cookies", null)
        //val cookies: Map<String, String> =
        //    this.intent.getSerializableExtra("cookies") as Map<String, String>

        val position: Int = this.intent.getIntExtra("position", -1)
        var devicesList: MutableList<Device> = getDevicesList()
        val device: Device = devicesList[position]

        var startTimeDate = ""
        var startTimeMs = ""
        var endTimeMs = ""

        var ruleUrlRequestString =
            "rule?starttimems=$startTimeDate&starttime=$startTimeMs&endtime=$endTimeMs"

        val factor: Int = this.resources.displayMetrics.density.toInt()
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, factor * 700)
        this.title = device.name


        val startDate: TextView = findViewById(R.id.startDate)
        startDate.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        val startTime: TextView = findViewById(R.id.startTime)
        val endTime: TextView = findViewById(R.id.endTime)
        val tempCheckBox: CheckBox = findViewById(R.id.checkBox)
        val tempMinTV: TextView = findViewById(R.id.minTempTV)
        val tempMaxTV: TextView = findViewById(R.id.maxTempTV)
        val tempMinET: EditText = findViewById(R.id.minTempET)
        val tempMaxET: EditText = findViewById(R.id.maxTempET)
        val temp2CheckBox: CheckBox = findViewById(R.id.checkBox2)
        val tempViews: ArrayList<View> = ArrayList()

        tempViews.add(tempMinTV)
        tempViews.add(tempMinET)
        tempViews.add(tempMaxTV)
        tempViews.add(tempMaxET)
        tempViews.add(temp2CheckBox)

        val removeRule: Button = findViewById(R.id.removeCurrentFileDialogBtn)
        val cancelBtn: Button = findViewById(R.id.cancelDialogBtn)
        val okBtn: Button = findViewById(R.id.okDialogBtn)

        val currentTimeWithSeconds: String = Calendar.getInstance().time.toString().split(" ")[3]

        @Suppress("SpellCheckingInspection")
        var currTimeSplitted: List<String> = currentTimeWithSeconds.split(":")

        val currentTimeWithoutSeconds: String = currTimeSplitted[0] + ":" + currTimeSplitted[1]

        fun addOneHour(hour: String): Int {
            var addHour: Int = (hour.toInt() + 1)
            if (addHour >= 24) addHour = 0
            return addHour
        }

        val addHour: Int = addOneHour(currTimeSplitted[0])

        val currentTimeWithoutSeconds1: String

        fun fixTimeFormat(hour: String, minute: String): String {
            var minutes: String = minute
            if (minutes[0] != '0' && minutes.toInt() < 10) minutes = "0$minutes"

            return if (hour.toInt() < 10) "0$hour:$minutes"
            else "$hour:$minutes"
        }

        currentTimeWithoutSeconds1 = fixTimeFormat(addHour.toString(), currTimeSplitted[1])

        startTime.text = currentTimeWithoutSeconds
        endTime.text = currentTimeWithoutSeconds1

        if (device.startDate > -1 && device.startTimeHHmm > -1 && device.endTimeHHmm > -1) {
            val c: Calendar = Calendar.getInstance()
            c.timeInMillis = device.startDate
            startDate.text = SimpleDateFormat("dd.MM.yyyy")
                .format(c.time)
            startTime.text = fixTimeFormat(
                (floor(device.startTimeHHmm / 3600000.0)).toInt().toString(),
                (((device.startTimeHHmm - (floor(device.startTimeHHmm / 3600000.0) * 3600000)) / 60000)).toInt()
                    .toString()
            )
            endTime.text = fixTimeFormat(
                (floor(device.endTimeHHmm / 3600000.0)).toInt().toString(),
                (((device.endTimeHHmm - (floor(device.endTimeHHmm / 3600000.0) * 3600000)) / 60000)).toInt()
                    .toString()
            )
        }

        when (device) {
            is WaterHeater -> {
                if (device.tempNotifs) {
                    tempCheckBox.isChecked = true
                    for (v: View in tempViews) {
                        v.isEnabled = !v.isEnabled
                    }
                    minTempET.setText(device.minTemp.toString())
                    maxTempET.setText(device.maxTemp.toString())
                }
                if (device.autoRegulateTemperature) {
                    temp2CheckBox.isChecked = true
                }
            }
            is Heater -> {
                if (device.tempNotifs) {
                    tempCheckBox.isChecked = true
                    for (v: View in tempViews) {
                        v.isEnabled = !v.isEnabled
                    }
                    minTempET.setText(device.minTemp.toString())
                    maxTempET.setText(device.maxTemp.toString())
                }
                if (device.autoRegulateTemperature) {
                    temp2CheckBox.isChecked = true
                }
            }
            is AirConditioner -> {
                if (device.tempNotifs) {
                    tempCheckBox.isChecked = true
                    for (v: View in tempViews) {
                        v.isEnabled = !v.isEnabled
                    }
                    minTempET.setText(device.minTemp.toString())
                    maxTempET.setText(device.maxTemp.toString())
                }
                if (device.autoRegulateTemperature) {
                    temp2CheckBox.isChecked = true
                }
            }
        }

        startDate.setOnClickListener {
            // Get Current Date
            val c: Calendar = Calendar.getInstance()
            val year: Int = c.get(Calendar.YEAR)
            val month: Int = c.get(Calendar.MONTH)
            val day: Int = c.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog =
                DatePickerDialog(
                    this,
                    DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                        val dateString: String =
                            dayOfMonth.toString() + "." + (monthOfYear + 1) + "." + year
                        startDate.text = dateString
                    }, year, month, day
                )

            datePickerDialog.datePicker.minDate = c.timeInMillis // Past dates can't be chosen
            datePickerDialog.show()
        }

        fun timeOnClickListener(time: TextView) {
            var hours: Int
            val hour = 0
            val minutes = 0

            val timePickerDialog =
                TimePickerDialog(
                    this,
                    TimePickerDialog.OnTimeSetListener { _, hour, minutes ->
                        time.text = fixTimeFormat(hour.toString(), minutes.toString())
                        hours = addOneHour(hour.toString())
                        if (time.id == R.id.startTime) endTime.text =
                            fixTimeFormat(hours.toString(), minutes.toString())
                    }, hour, minutes, true
                )

            currTimeSplitted = time.text.split(":")
            timePickerDialog.updateTime(currTimeSplitted[0].toInt(), currTimeSplitted[1].toInt())
            timePickerDialog.show()
        }

        startTime.setOnClickListener {
            timeOnClickListener(startTime)
        }

        endTime.setOnClickListener {
            timeOnClickListener(endTime)
        }

        removeRule.setOnClickListener {
            startTimeDate = "-1"
            startTimeMs = "-1"
            endTimeMs = "-1"

            device.startDate = -1
            device.startTimeHHmm = -1
            device.endTimeHHmm = -1

            when (device) {
                is WaterHeater -> {
                    device.tempNotifs = false
                    device.autoRegulateTemperature = false
                }
                is Heater -> {
                    device.tempNotifs = false
                    device.autoRegulateTemperature = false
                }
                is AirConditioner -> {
                    device.tempNotifs = false
                    device.autoRegulateTemperature = false
                }
            }


            devicesList[position] = device
            setDevicesList("devicesList", devicesList)

            ruleUrlRequestString =
                "rule?starttimems=$startTimeDate&starttime=$startTimeMs&endtime=$endTimeMs"

            println(ruleUrlRequestString)

            GlobalScope.launch(IO) {
                Jsoup.connect(Secrets().url + ruleUrlRequestString)
                    .cookie("ARDUINOSESSIONID", cookie)
                    .get()
            }

            finish()
            startActivity(intent)
        }

        tempCheckBox.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            for (v: View in tempViews) {
                v.isEnabled = !v.isEnabled
            }
        }

        cancelBtn.setOnClickListener {
            //onBackPressed()
            this.finish()
        }

        okBtn.setOnClickListener {
            var canFinish = true
            val calendar: Calendar = Calendar.getInstance()
            println("1: " + startDate.text)
            val date: List<String> = startDate.text.split(".")
            //for (j in 0..2) {
            //    println("date:" + date[j])
            //}
            val time: List<String> = startTime.text.split(":")
            calendar.set(
                date[2].toInt(),
                date[1].toInt(),
                date[0].toInt(),
                time[0].toInt(),
                time[1].toInt()
            )
            calendar.add(
                Calendar.MONTH,
                -1
            ) // For some odd reason, it is adding +1 month to the date after the previous line
            startTimeDate = calendar.timeInMillis.toString()

            val endtime: List<String> = endTime.text.split(":")
            startTimeMs = (time[0].toInt() * 3600000 + time[1].toInt() * 60000).toString()
            endTimeMs = (endtime[0].toInt() * 3600000 + endtime[1].toInt() * 60000).toString()

            ruleUrlRequestString =
                "rule?starttimems=$startTimeDate&starttime=$startTimeMs&endtime=$endTimeMs"

            GlobalScope.launch(IO) {
                Jsoup.connect(Secrets().url + ruleUrlRequestString)
                    .cookie("ARDUINOSESSIONID", cookie)
                    .get()
            }

            devicesList[position].startDate = startTimeDate.toLong()
            devicesList[position].startTimeHHmm = startTimeMs.toLong()
            devicesList[position].endTimeHHmm = endTimeMs.toLong()


            if (tempCheckBox.isChecked) {
                try {
                    when (device) {
                        is WaterHeater -> {
                            device.minTemp = minTempET.text.toString().toDouble()
                            device.maxTemp = maxTempET.text.toString().toDouble()
                            if (device.minTemp > device.maxTemp) throw NumberFormatException()
                        }
                        is Heater -> {
                            device.minTemp = minTempET.text.toString().toDouble()
                            device.maxTemp = maxTempET.text.toString().toDouble()
                            if (device.minTemp > device.maxTemp) throw NumberFormatException()
                        }
                        is AirConditioner -> {
                            device.minTemp = minTempET.text.toString().toDouble()
                            device.maxTemp = maxTempET.text.toString().toDouble()
                            if (device.minTemp > device.maxTemp) throw NumberFormatException()
                        }
                    }
                } catch (e: NumberFormatException) {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Invalid temperature values")
                    builder.setMessage("Please enter valid temperature values!\n\nNote: Minimum temperature value has to be smaller or equal to maximum temperature value.")

                    builder.setPositiveButton(android.R.string.yes) { dialog, which ->
                    }

                    canFinish = false
                    builder.show()

                } catch (e: Exception) {
                    System.err.println("Unknown error while getting temperature values from the DeviceDialog EditTexts")
                    e.printStackTrace()
                }

                var notificationDetails: ArrayList<String> = arrayListOf()
                when (device) {
                    is WaterHeater -> {
                        device.tempNotifs = true
                        device.autoRegulateTemperature = temp2CheckBox.isChecked
                        devicesList[position] = device
                        setDevicesList("devicesList", devicesList)
                        notificationDetails = arrayListOf(
                            "Temperature not in range!",
                            device.name + " is currently at " + device.temperature.toString() + "°C"
                        )
                    }
                    is Heater -> {
                        device.tempNotifs = true
                        device.autoRegulateTemperature = temp2CheckBox.isChecked
                        devicesList[position] = device
                        setDevicesList("devicesList", devicesList)
                        notificationDetails = arrayListOf(
                            "Temperature not in range!",
                            device.name + " is currently at " + device.temperature.toString() + "°C"
                        )
                    }
                    is AirConditioner -> {
                        device.tempNotifs = true
                        device.autoRegulateTemperature = temp2CheckBox.isChecked
                        if (temp2CheckBox.isChecked) device.autoRegulateTemperature = true
                        devicesList[position] = device
                        setDevicesList("devicesList", devicesList)
                        notificationDetails = arrayListOf(
                            "Temperature not in range!",
                            device.name + " is currently at " + device.temperature.toString() + "°C"
                        )
                    }
                }

                val broadcastIntent = Intent(this, CheckTemperatureAlarmReceiver::class.java)
                broadcastIntent.putStringArrayListExtra("notif", notificationDetails)
                //broadcastIntent.putExtra("cookies", cookies as Serializable)

                val alarmManager =
                    applicationContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                val pendingIntent =
                    PendingIntent.getBroadcast(
                        this,
                        0,
                        broadcastIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                if (pendingIntent != null && alarmManager != null) {
                    alarmManager.cancel(pendingIntent)
                }

                alarmManager?.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime(),
                    pendingIntent
                )
            } else {
                when (device) {
                    is WaterHeater -> device.tempNotifs = false
                    is Heater -> device.tempNotifs = false
                    is AirConditioner -> device.tempNotifs = false
                }
            }

            setDevicesList("devicesList", devicesList)
            if (canFinish) this.finish()
        }

    }

    fun getDevicesList(): MutableList<Device> {
        var arrayItems: MutableList<Device> = mutableListOf()
        val serializedObject: String? =
            devicesListPreferences.getString("devicesList", null)
        if (serializedObject != null) {
            var gson: Gson = GsonBuilder().registerTypeAdapterFactory(
                RuntimeTypeAdapterFactory
                    .of(Device::class.java)
                    .registerSubtype(WaterHeater::class.java)
                    .registerSubtype(Heater::class.java)
                    .registerSubtype(AirConditioner::class.java)
                        as RuntimeTypeAdapterFactory<Device>
            ).create()
            val type: Type =
                TypeToken.getParameterized(MutableList::class.java, Device::class.java).type
            arrayItems = gson.fromJson(serializedObject, type)
        }
        return arrayItems
    }


    fun setDevicesList(key: String, list: MutableList<Device>) {
        var gson: Gson = GsonBuilder().registerTypeAdapterFactory(
            RuntimeTypeAdapterFactory
                .of(Device::class.java)
                .registerSubtype(WaterHeater::class.java)
                .registerSubtype(Heater::class.java)
                .registerSubtype(AirConditioner::class.java)
                    as RuntimeTypeAdapterFactory<Device>
        ).create()
        val type = TypeToken.getParameterized(MutableList::class.java, Device::class.java).type
        val json: String = gson.toJson(list, type)

        set(key, json)
    }

    fun set(key: String, value: String) {
        editor.putString(key, value)
        editor.commit()
    }

}
