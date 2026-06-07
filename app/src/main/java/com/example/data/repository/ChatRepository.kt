package com.example.data.repository

import com.example.data.local.MessageDao
import com.example.data.local.UserDao
import com.example.data.model.Message
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val userDao: UserDao,
    private val messageDao: MessageDao
) {
    val allUsersFlow: Flow<List<User>> = userDao.getAllUsersFlow()

    suspend fun getAllUsers(): List<User> {
        return userDao.getAllUsers()
    }

    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }

    fun observeUserByEmail(email: String): Flow<User?> {
        return userDao.observeUserByEmail(email)
    }

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }

    fun observeMessagesBetween(user1: String, user2: String): Flow<List<Message>> {
        return messageDao.observeMessagesBetweenUsers(user1, user2)
    }

    fun observeAllMessagesForUser(userEmail: String): Flow<List<Message>> {
        return messageDao.observeAllMessagesForUser(userEmail)
    }

    suspend fun getAllMessagesForUser(userEmail: String): List<Message> {
        return messageDao.getAllMessagesForUser(userEmail)
    }

    suspend fun insertMessage(message: Message): Long {
        return messageDao.insertMessage(message)
    }

    suspend fun markMessagesAsRead(senderEmail: String, receiverEmail: String) {
        messageDao.markMessagesAsRead(senderEmail, receiverEmail)
    }

    suspend fun deleteChatBetween(user1: String, user2: String) {
        messageDao.deleteChatBetween(user1, user2)
    }
}
