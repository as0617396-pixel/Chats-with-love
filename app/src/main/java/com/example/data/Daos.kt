package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends")
    fun getAllFriendsFlow(): Flow<List<Friend>>

    @Query("SELECT * FROM friends WHERE id = :id")
    suspend fun getFriendById(id: String): Friend?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: Friend)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<Friend>)

    @Query("UPDATE friends SET status = :status WHERE id = :id")
    suspend fun updateFriendStatus(id: String, status: String)

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteFriend(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE channelId IS NULL AND ((senderId = :myId AND receiverId = :friendId) OR (senderId = :friendId AND receiverId = :myId)) ORDER BY timestamp ASC")
    fun getPrivateMessagesFlow(myId: String, friendId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE channelId = :channelId ORDER BY timestamp ASC")
    fun getChannelMessagesFlow(channelId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: Int)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}
