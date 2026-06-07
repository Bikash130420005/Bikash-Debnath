package com.example.data.model

data class Conversation(
    val contact: User,
    val lastMessage: Message,
    val unreadCount: Int = 0
)
