package com.votime.app

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Bundle
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class WhatsAppListenerService : NotificationListenerService() {

    // Apna Google Apps Script Webhook URL yahan dalein:
    private val WEBHOOK_URL = "https://script.google.com/macros/s/AKfycby-W_r-BzVK46tP1ZWC6vdL4Jfcc7RFJiR21aIdkx2sB2HQKvy_HMD2HEkVqFvP1wo/exec"
    
    // Apne WhatsApp Group ka exact ya keyword naam yahan dalein:
    private val TARGET_GROUP = "Attendance"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn?.packageName == "com.whatsapp") {
            val extras: Bundle = sbn.notification.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""

            if (title.contains(TARGET_GROUP, ignoreCase = true) || 
                extras.getString("android.subText")?.contains(TARGET_GROUP, ignoreCase = true) == true) {
                
                var sender = title
                var msg = text

                if (text.contains(":")) {
                    val parts = text.split(":", limit = 2)
                    sender = parts[0].trim()
                    msg = parts[1].trim()
                }

                val status = parseStatus(msg)
                val curTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val curDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                thread {
                    sendToSheet(curDate, sender, status, curTime, msg)
                }

                AttendanceStore.updateStatus(this, sender, status, curTime)
            }
        }
    }

    private fun parseStatus(msg: String): String {
        val upper = msg.uppercase()
        return when {
            "IN" in upper && "OUT" not in upper -> "IN OFFICE"
            "BREAK" in upper || "LUNCH" in upper || "TEA" in upper -> "ON BREAK"
            "OUT" in upper -> "LOGGED OUT"
            else -> "OTHER"
        }
    }

    private fun sendToSheet(date: String, sender: String, status: String, time: String, rawMsg: String) {
        if (WEBHOOK_URL.startsWith("http")) {
            try {
                val url = URL(WEBHOOK_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")

                val json = JSONObject().apply {
                    put("date", date)
                    put("sender", sender)
                    put("status", status)
                    put("time", time)
                    put("rawMsg", rawMsg)
                }

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(json.toString())
                writer.flush()
                writer.close()
                conn.responseCode
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}