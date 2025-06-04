package com.example.myproject

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Напоминание"
        val message = intent.getStringExtra("message") ?: "Время события приближается!"
        val reminderId = intent.getIntExtra("reminder_id", 0)

        NotificationHelper(context).showNotification(title, message, reminderId)
    }
}