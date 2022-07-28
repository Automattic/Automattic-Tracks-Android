package com.example.sampletracksapp.performance

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Track(
    @PrimaryKey
    val id: String
)
