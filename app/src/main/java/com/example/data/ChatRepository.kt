package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatRepository(
    private val friendDao: FriendDao,
    private val messageDao: MessageDao
) {
    val allFriends: Flow<List<Friend>> = friendDao.getAllFriendsFlow()

    fun getPrivateMessages(friendId: String): Flow<List<Message>> {
        return messageDao.getPrivateMessagesFlow("me", friendId)
    }

    fun getChannelMessages(channelId: String): Flow<List<Message>> {
        return messageDao.getChannelMessagesFlow(channelId)
    }

    suspend fun insertMessage(message: Message) = withContext(Dispatchers.IO) {
        messageDao.insertMessage(message)
    }

    suspend fun updateFriendStatus(id: String, status: String) = withContext(Dispatchers.IO) {
        friendDao.updateFriendStatus(id, status)
    }

    suspend fun insertFriend(friend: Friend) = withContext(Dispatchers.IO) {
        friendDao.insertFriend(friend)
    }

    suspend fun deleteFriend(id: String) = withContext(Dispatchers.IO) {
        friendDao.deleteFriend(id)
    }

    // Handles logic of sending a private message (including E2EE cipher prep if enabled)
    suspend fun sendPrivateMessage(
        friendId: String,
        content: String,
        isEncrypted: Boolean,
        stickerName: String?
    ) = withContext(Dispatchers.IO) {
        val payload = if (isEncrypted) {
            CryptoHelper.encrypt(content, "me_and_$friendId")
        } else {
            null
        }

        val message = Message(
            senderId = "me",
            receiverId = friendId,
            content = payload?.cipherText ?: content,
            encryptedContent = payload?.hexProof,
            isEncrypted = isEncrypted,
            stickerName = stickerName,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(message)
    }

    // Handles channels
    suspend fun sendChannelMessage(
        channelId: String,
        content: String,
        isEncrypted: Boolean,
        stickerName: String?
    ) = withContext(Dispatchers.IO) {
        val payload = if (isEncrypted) {
            CryptoHelper.encrypt(content, "channel_$channelId")
        } else {
            null
        }

        val message = Message(
            senderId = "me",
            receiverId = channelId,
            content = payload?.cipherText ?: content,
            encryptedContent = payload?.hexProof,
            isEncrypted = isEncrypted,
            stickerName = stickerName,
            timestamp = System.currentTimeMillis(),
            channelId = channelId
        )
        messageDao.insertMessage(message)
    }
}
