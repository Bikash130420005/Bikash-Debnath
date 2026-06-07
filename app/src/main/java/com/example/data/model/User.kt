package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val name: String,
    val avatarColorIndex: Int, // index for color palette to display custom-styled colored circles
    val bio: String = "",
    val isBot: Boolean = false,
    val lastActive: Long = System.currentTimeMillis()
)
