package com.example.myproject

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Получаем сохраненные напоминания из SharedPreferences
            val sharedPref = context.getSharedPreferences("RemindersPrefs", Context.MODE_PRIVATE)
            val remindersJson = sharedPref.getString("reminders_list", "[]")

            val type = object : TypeToken<List<Reminder>>() {}.type
            val reminders = Gson().fromJson<List<Reminder>>(remindersJson, type)

            // Планируем уведомления для каждого напоминания
            val scheduler = ReminderScheduler(context)
            reminders.forEach { reminder ->
                scheduler.scheduleReminders(reminder)
            }
        }
    }
}