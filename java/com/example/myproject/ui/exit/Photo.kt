package com.example.myproject.ui.exit

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val animalId: Int,
    val image: ByteArray // вот здесь — фотография
)
