package com.example.myproject

import com.google.gson.annotations.SerializedName

data class UserIdResponse(
    @SerializedName("id_user")
    val id_user: Int
)
