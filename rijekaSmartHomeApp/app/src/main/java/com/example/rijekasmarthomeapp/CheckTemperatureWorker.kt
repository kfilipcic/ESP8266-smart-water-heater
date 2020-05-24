package com.example.rijekasmarthomeapp

import android.app.Notification.DEFAULT_ALL
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.hardware.camera2.params.RggbChannelVector.RED
import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION
import android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE
import android.media.RingtoneManager.TYPE_NOTIFICATION
import android.media.RingtoneManager.getDefaultUri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationCompat.PRIORITY_MAX
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document


class CheckTemperatureWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun doWork(): Result {
        try {
            val cookieName: String? = inputData.getString("cookieName")
            val cookieValue: String? = inputData.getString("cookieValue")


            try {
                val temperatureHTML: Document =
                    Jsoup.connect("http://psih.duckdns.org/water_heater_1_temperature.html")
                        .cookie(cookieName, cookieValue)
                        .get()
                val temperaturePage = JSONObject(temperatureHTML.body().text())

               val currentTemp: Double = temperaturePage.get("water_heater").toString().toDouble()
                if (currentTemp > 10) {

                    val cookies: MutableMap<String, String> = mutableMapOf<String, String>()

                    if (cookieName != null) {
                        if (cookieValue != null) {
                            cookies[cookieName] = cookieValue
                        }
                    }
                    sendNotification(1, cookies)
                }

            } catch (e: Exception) {
               e.printStackTrace()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Indicate whether the task finished successfully with the Result
        return Result.success()
    }
    private fun sendNotification(id: Int, cookies: Map<String, String>) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra(NOTIFICATION_ID, id)
        //intent.putExtra("Map", cookies as Serializable)

        val notificationManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val titleNotification = applicationContext.getString(R.string.alert_channel_name)
        val subtitleNotification = applicationContext.getString(R.string.alert_channel_description)
        val pendingIntent = getActivity(applicationContext, 0, intent, 0)
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.boiler_on)
            .setContentTitle(titleNotification).setContentText(subtitleNotification)
            .setDefaults(DEFAULT_ALL).setContentIntent(pendingIntent).setAutoCancel(true)

        notification.priority = PRIORITY_HIGH

        if (SDK_INT >= O) {
            notification.setChannelId(NOTIFICATION_CHANNEL)

            val ringtoneManager = getDefaultUri(TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder().setUsage(USAGE_NOTIFICATION_RINGTONE)
                .setContentType(CONTENT_TYPE_SONIFICATION).build()

            val channel =
                NotificationChannel(NOTIFICATION_CHANNEL, NOTIFICATION_NAME, NotificationManager.IMPORTANCE_DEFAULT)

            channel.enableLights(true)
            channel.lightColor = RED
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            channel.setSound(ringtoneManager, audioAttributes)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(id, notification.build())
    }

    companion object {
        const val NOTIFICATION_ID = "appName_notification_id"
        const val NOTIFICATION_NAME = "appName"
        const val NOTIFICATION_CHANNEL = "appName_channel_01"
        const val NOTIFICATION_WORK = "appName_notification_work"
    }

}
