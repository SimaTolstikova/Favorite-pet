package com.example.myproject

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminders(reminder: Reminder) {
        val eventTime = reminder.getEventTimeInMillis()

        // Уведомление за 24 часа
        scheduleNotification(
            triggerTime = eventTime - 24 * 60 * 60 * 1000,
            requestCode = reminder.id_reminder * 10 + 1, // Уникальный код для каждого уведомления
            title = reminder.heading,
            message = "До события '${reminder.heading}' осталось 24 часа",
            reminderId = reminder.id_reminder
        )

        // Уведомление за 12 часов
        scheduleNotification(
            eventTime - 12 * 60 * 60 * 1000,
            reminder.id_reminder * 10 + 2,
            reminder.heading,
            "До события '${reminder.heading}' осталось 12 часов",
            reminder.id_reminder
        )

        // Уведомление за 6 часов
        scheduleNotification(
            eventTime - 6 * 60 * 60 * 1000,
            reminder.id_reminder * 10 + 3,
            reminder.heading,
            "До события '${reminder.heading}' осталось 6 часов",
            reminder.id_reminder
        )

        // Уведомление за 1 час
        scheduleNotification(
            eventTime - 60 * 60 * 1000,
            reminder.id_reminder * 10 + 4,
            reminder.heading,
            "До события '${reminder.heading}' остался 1 час",
            reminder.id_reminder
        )
    }

    private fun scheduleNotification(
        triggerTime: Long,
        requestCode: Int,
        title: String,
        message: String,
        reminderId: Int
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.yourpackage.REMINDER_ACTION"
            putExtra("title", title)
            putExtra("message", message)
            putExtra("reminder_id", reminderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelReminders(reminderId: Int) {
        // Отменяем все 4 уведомления
        for (i in 1..4) {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId * 10 + i,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}