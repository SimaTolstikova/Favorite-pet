package com.example.myproject

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: T
)
