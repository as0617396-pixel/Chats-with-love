package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey val id: String,
    val name: String,
    val avatar: String, // Emoji or custom character representation
    val bio: String,
    val status: String, // "RECRUIT" (Open World discovery), "SENT", "PENDING_ACCEPT" (needs acceptance), "FRIENDS" (Accepted, permission to video call)
    val isOnline: Boolean = true,
    val lastSeenUnit: String = "online"
) : Serializable

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val encryptedContent: String?,  // Hex ciphertext representation
    val isEncrypted: Boolean,
    val stickerName: String?,       // Name of sticker if any, null otherwise
    val timestamp: Long = System.currentTimeMillis(),
    val channelId: String? = null    // Null for private 1-on-1 chats, otherwise e.g. "open_general"
) : Serializable
