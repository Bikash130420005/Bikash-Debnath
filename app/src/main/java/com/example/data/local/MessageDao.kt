package com.example.data.local

import androidx.room.*
import com.example.data.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("""
        SELECT * FROM messages 
        WHERE (senderEmail = :user1 AND receiverEmail = :user2) 
           OR (senderEmail = :user2 AND receiverEmail = :user1) 
        ORDER BY timestamp ASC
    """)
    fun observeMessagesBetweenUsers(user1: String, user2: String): Flow<List<Message>>

    @Query("""
        SELECT * FROM messages 
        WHERE senderEmail = :userEmail OR receiverEmail = :userEmail 
        ORDER BY timestamp DESC
    """)
    fun observeAllMessagesForUser(userEmail: String): Flow<List<Message>>

    @Query("""
        SELECT * FROM messages 
        WHERE senderEmail = :userEmail OR receiverEmail = :userEmail 
        ORDER BY timestamp DESC
    """)
    suspend fun getAllMessagesForUser(userEmail: String): List<Message>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Query("UPDATE messages SET isRead = 1 WHERE senderEmail = :senderEmail AND receiverEmail = :receiverEmail")
    suspend fun markMessagesAsRead(senderEmail: String, receiverEmail: String)

    @Query("DELETE FROM messages WHERE (senderEmail = :user1 AND receiverEmail = :user2) OR (senderEmail = :user2 AND receiverEmail = :user1)")
    suspend fun deleteChatBetween(user1: String, user2: String)
}
