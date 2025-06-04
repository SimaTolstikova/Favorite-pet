package com.example.myproject

import java.io.Serializable

data class Animal(
    val id_animal: Int,
    val id_user: Int,
    val nickname: String,
    val id_type: Int,
    val date: String,
    val id_gender: Int,
    val breed: String,
    val photo: String
) : Serializable
