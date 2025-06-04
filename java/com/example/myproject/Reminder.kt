package com.example.myproject

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale

data class Reminder(
    @SerializedName("id_reminder") val id_reminder: Int,
    @SerializedName("id_user") val id_user: Int,
    @SerializedName("id_animal") val id_animal: Int,
    @SerializedName("heading") val heading: String,
    @SerializedName("text") val text: String,
    @SerializedName("date") val date: String,
    @SerializedName("time") val time: String
) {
    // Метод для получения времени события в миллисекундах
    fun getEventTimeInMillis(): Long {
        val dateTimeString = "$date $time"
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return format.parse(dateTimeString)?.time ?: 0L
    }
}