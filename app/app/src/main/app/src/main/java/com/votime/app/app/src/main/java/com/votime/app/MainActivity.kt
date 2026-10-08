package com.votime.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import android.graphics.Color

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
        }

        val titleView = TextView(this).apply {
            text = "VO Time - Live Dashboard"
            textSize = 22f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 30)
        }
        layout.addView(titleView)

        val btnPerm = Button(this).apply {
            text = "1. Enable WhatsApp Notification Access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        layout.addView(btnPerm)

        val liveStatusView = TextView(this).apply {
            textSize = 16f
            setPadding(0, 40, 0, 20)
            text = AttendanceStore.getLiveStatusText(this@MainActivity)
        }
        layout.addView(liveStatusView)

        val btnRefresh = Button(this).apply {
            text = "Refresh Dashboard"
            setOnClickListener {
                liveStatusView.text = AttendanceStore.getLiveStatusText(this@MainActivity)
            }
        }
        layout.addView(btnRefresh)

        setContentView(layout)
    }
}

object AttendanceStore {
    fun updateStatus(context: android.content.Context, sender: String, status: String, time: String) {
        val sp = context.getSharedPreferences("vo_time_data", android.content.Context.MODE_PRIVATE)
        sp.edit().putString(sender, "$status at $time").apply()
    }

    fun getLiveStatusText(context: android.content.Context): String {
        val sp = context.getSharedPreferences("vo_time_data", android.content.Context.MODE_PRIVATE)
        val all = sp.all
        if (all.isEmpty()) return "No records received today yet.\nMake sure notification access is allowed."

        val sb = StringBuilder("--- CURRENT STATUS ---\n\n")
        all.forEach { (user, info) ->
            val icon = when {
                info.toString().contains("IN OFFICE") -> "🟢"
                info.toString().contains("ON BREAK") -> "🟡"
                info.toString().contains("LOGGED OUT") -> "🔴"
                else -> "⚪"
            }
            sb.append("$icon $user : $info\n")
        }
        return sb.toString()
    }
}