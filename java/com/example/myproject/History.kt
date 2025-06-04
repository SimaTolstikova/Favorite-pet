package com.example.myproject

import com.google.gson.annotations.SerializedName

data class History(
    @SerializedName("id_history") val id_history: Int,
    @SerializedName("id_user") val id_user: Int,
    @SerializedName("id_animal") val id_animal: Int,
    @SerializedName("heading") val heading: String,
    @SerializedName("text") val text: String,
    @SerializedName("dateStart") val dateStart: String,
    @SerializedName("dateEnd") val dateEnd: String
)
