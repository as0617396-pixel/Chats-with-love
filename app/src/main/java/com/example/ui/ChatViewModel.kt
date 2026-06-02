package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = ChatRepository(db.friendDao(), db.messageDao())

    // UI Navigation tabs: "WORLD" (Channel chat), "FRIENDS" (1-on-1 private DMs), "DISCOVER" (Recruit & Add), "VAULT" (E2EE Key test)
    private val _currentTab = MutableStateFlow("WORLD")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Observable states from db
    val friends: StateFlow<List<Friend>> = repository.allFriends
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active screen selection contexts
    private val _activeChatFriend = MutableStateFlow<Friend?>(null)
    val activeChatFriend: StateFlow<Friend?> = _activeChatFriend.asStateFlow()

    private val _activeChannelId = MutableStateFlow<String>("open_general")
    val activeChannelId: StateFlow<String> = _activeChannelId.asStateFlow()

    // Global toggle for E2EE mode before typing!
    private val _isEncryptionEnabled = MutableStateFlow(false)
    val isEncryptionEnabled: StateFlow<Boolean> = _isEncryptionEnabled.asStateFlow()

    // Active messaging stream
    val privateMessages: Flow<List<Message>> = _activeChatFriend.flatMapLatest { friend ->
        if (friend != null) {
            repository.getPrivateMessages(friend.id)
        } else {
            flowOf(emptyList())
        }
    }

    val channelMessages: Flow<List<Message>> = _activeChannelId.flatMapLatest { channelId ->
        repository.getChannelMessages(channelId)
    }

    // Video Call status
    private val _activeVideoCallFriend = MutableStateFlow<Friend?>(null)
    val activeVideoCallFriend: StateFlow<Friend?> = _activeVideoCallFriend.asStateFlow()

    private val _videoCallActive = MutableStateFlow(false)
    val videoCallActive: StateFlow<Boolean> = _videoCallActive.asStateFlow()

    private val _videoCallSparkles = MutableStateFlow("None") // "None", "Sparkles ✨", "Flower Crown 🌸", "Cute Kitty 🐱", "Blushing Cheeks 🥰"
    val videoCallSparkles: StateFlow<String> = _videoCallSparkles.asStateFlow()

    private val _micMuted = MutableStateFlow(false)
    val micMuted: StateFlow<Boolean> = _micMuted.asStateFlow()

    private val _cameraMuted = MutableStateFlow(false)
    val cameraMuted: StateFlow<Boolean> = _cameraMuted.asStateFlow()

    // Trigger for the lovely bouncing floating emoji shower!
    private val _emojiShowerTriggers = MutableStateFlow(0)
    val emojiShowerTriggers: StateFlow<Int> = _emojiShowerTriggers.asStateFlow()

    private val _emojiShowerType = MutableStateFlow("💖")
    val emojiShowerType: StateFlow<String> = _emojiShowerType.asStateFlow()

    // Decryption status for clicking individual messages in private chat
    private val _decryptedMessagesTemp = MutableStateFlow<Map<Int, String>>(emptyMap())
    val decryptedMessagesTemp: StateFlow<Map<Int, String>> = _decryptedMessagesTemp.asStateFlow()

    // Live Snapchat-like activity/typing state
    private val _isFriendTyping = MutableStateFlow(false)
    val isFriendTyping: StateFlow<Boolean> = _isFriendTyping.asStateFlow()

    // Setup custom profile
    private val _myProfileName = MutableStateFlow("My SweetCupcake 🧁")
    val myProfileName: StateFlow<String> = _myProfileName.asStateFlow()

    fun updateProfileName(name: String) {
        if (name.isNotBlank()) {
            _myProfileName.value = name
        }
    }

    // User interaction handlers
    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    fun selectActiveFriend(friend: Friend?) {
        _activeChatFriend.value = friend
        if (friend != null) {
            _currentTab.value = "CHATTING_FRIEND"
        }
    }

    fun selectActiveChannel(channelId: String) {
        _activeChannelId.value = channelId
        _currentTab.value = "WORLD"
    }

    fun toggleEncryption(enable: Boolean) {
        _isEncryptionEnabled.value = enable
    }

    fun toggleMic() {
        _micMuted.value = !_micMuted.value
    }

    fun toggleCamera() {
        _cameraMuted.value = !_cameraMuted.value
    }

    fun setVideoFilter(filter: String) {
        _videoCallSparkles.value = filter
    }

    fun triggerEmojiShower(emoji: String) {
        _emojiShowerType.value = emoji
        _emojiShowerTriggers.value += 1
    }

    // Sends private message & handles smart simulated interactive reply
    fun sendPrivateMessage(content: String, stickerName: String? = null) {
        val friendObj = _activeChatFriend.value ?: return
        if (content.isBlank() && stickerName == null) return

        val e2ee = _isEncryptionEnabled.value
        val originalText = content

        viewModelScope.launch {
            // Save user message
            repository.sendPrivateMessage(friendObj.id, content, e2ee, stickerName)

            // Show live typing status like snapchat!
            _isFriendTyping.value = true
            // Auto-trigger simulated response after delay (like real chatting friends)
            delay(2200)
            _isFriendTyping.value = false

            val replyText = generateSimulatedReply(friendObj, originalText, stickerName)
            val replySticker = if (originalText.contains("sticker", ignoreCase = true) || Math.random() < 0.25) {
                // Occasional sticker response
                listOf("love_spark", "hug_bear", "laugh_heart", "music_love", "sweet_bubble").random()
            } else {
                null
            }

            // Friend replies with E2EE matches!
            val replyPayLoad = if (e2ee) {
                CryptoHelper.encrypt(replyText, "me_and_${friendObj.id}")
            } else {
                null
            }

            val incomingMessage = Message(
                senderId = friendObj.id,
                receiverId = "me",
                content = replyPayLoad?.cipherText ?: replyText,
                encryptedContent = replyPayLoad?.hexProof,
                isEncrypted = e2ee,
                stickerName = replySticker,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(incomingMessage)
        }
    }

    // Sends channel message & handles quick reaction chatty replies
    fun sendChannelMessage(content: String, stickerName: String? = null) {
        val channelId = _activeChannelId.value
        if (content.isBlank() && stickerName == null) return

        val e2ee = _isEncryptionEnabled.value
        val originalText = content

        viewModelScope.launch {
            repository.sendChannelMessage(channelId, content, e2ee, stickerName)

            // In open world, others answer dynamically!
            delay(1500)
            val responder = friends.value.filter { it.status == "FRIENDS" || it.id == "teddy_sweet" || it.id == "bubble_kitt" }.randomOrNull()
            if (responder != null) {
                val replyText = "Wow! " + generateSimulatedReply(responder, originalText, stickerName)
                val replyPayload = if (e2ee) {
                    CryptoHelper.encrypt(replyText, "channel_$channelId")
                } else {
                    null
                }

                val incomingMsg = Message(
                    senderId = responder.id,
                    receiverId = channelId,
                    content = replyPayload?.cipherText ?: replyText,
                    encryptedContent = replyPayload?.hexProof,
                    isEncrypted = e2ee,
                    stickerName = if (Math.random() < 0.3) "sweet_bubble" else null,
                    timestamp = System.currentTimeMillis(),
                    channelId = channelId
                )
                repository.insertMessage(incomingMsg)
            }
        }
    }

    // Manual on-device interactive decryption tester
    fun interactivelyDecryptMessage(message: Message) {
        if (!message.isEncrypted) return
        viewModelScope.launch {
            val seed = if (message.channelId != null) "channel_${message.channelId}" else "me_and_${if (message.senderId == "me") message.receiverId else message.senderId}"
            val decrypted = CryptoHelper.decrypt(message.content, seed)
            _decryptedMessagesTemp.update {
                it + (message.id to decrypted)
            }
        }
    }

    // Interactive friend requests updates
    fun acceptFriendRequest(friendId: String) {
        viewModelScope.launch {
            repository.updateFriendStatus(friendId, "FRIENDS")
            // Send sweet welcome message in the conversation right away
            val welcomeMsg = Message(
                senderId = friendId,
                receiverId = "me",
                content = "Yay! Sparkles added! 💖 Message me, encrypt our secrets, or tap the video camera for digital dynamic filters! 🥰🌸",
                encryptedContent = null,
                isEncrypted = false,
                stickerName = "love_spark",
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(welcomeMsg)
        }
    }

    fun declineFriendRequest(friendId: String) {
        viewModelScope.launch {
            // Revert back to RECRUIT so they are in discovery deck, or deleted
            repository.updateFriendStatus(friendId, "RECRUIT")
        }
    }

    fun spawnRandomFriendRequestBot() {
        viewModelScope.launch {
            val names = listOf("Cherry Cupcake", "Starry Night", "Mango Slushy", "Lucky Clover", "Golden Honey", "Milky Tea")
            val avatars = listOf("🧁", "🌟", "🥭", "🍀", "🍯", "🧋")
            val bios = listOf(
                "Super cute bot that loves warm fluffy bubble gums! 🌸🍬",
                "Stargazing and dreaming about E2EE secure channels! ✨🔒",
                "Spreading sweet positive vibes. Tap accept to start chatting! 🥰",
                "Waving from a lovely clover garden! Besties forever 🍀",
                "Crafting gorgeous high-fidelity live audio & video effects! 👑🎨",
                "Brewing sweet jasmine bubble milk tea for you! 🧋✨"
            )
            val index = (0 until names.size).random()
            val cleanId = "bot_" + System.currentTimeMillis()
            val bot = Friend(
                id = cleanId,
                name = names[index] + " 🤖",
                avatar = avatars[index],
                bio = bios[index],
                status = "PENDING_ACCEPT", // Incoming request so we can Accept/Decline!
                isOnline = listOf(true, false).random()
            )
            repository.insertFriend(bot)
        }
    }

    fun recruitNewFriend(name: String, avatar: String, bio: String) {
        viewModelScope.launch {
            val cleanId = "recruit_" + name.replace(" ", "_").lowercase() + "_" + (10..99).random()
            val newFriend = Friend(
                id = cleanId,
                name = name,
                avatar = avatar.ifBlank { "🌸" },
                bio = bio.ifBlank { "Loves meeting wonderful friends in this open world room!" },
                status = "RECRUIT",
                isOnline = true
            )
            repository.insertFriend(newFriend)
        }
    }

    fun toggleFriendOnlineStatus(friendId: String) {
        viewModelScope.launch {
            val current = db.friendDao().getFriendById(friendId)
            if (current != null) {
                val updated = current.copy(isOnline = !current.isOnline)
                repository.insertFriend(updated)
            }
        }
    }

    fun sendFriendRequestToRecruit(friendId: String) {
        viewModelScope.launch {
            repository.updateFriendStatus(friendId, "SENT")
            // Simulate self-acceptance in a short sweet interactive simulation after 4 seconds!
            delay(4000)
            val current = db.friendDao().getFriendById(friendId)
            if (current != null && current.status == "SENT") {
                repository.updateFriendStatus(friendId, "FRIENDS")
                val acceptanceSuccess = Message(
                    senderId = friendId,
                    receiverId = "me",
                    content = "Woohoo! I accepted your request! We are now official besties! 🥰 Video calling permission is UNLOCKED! Pinky promise me to E2EE encrypt our chats. 🔒🗝️",
                    encryptedContent = null,
                    isEncrypted = false,
                    stickerName = "sweet_bubble",
                    timestamp = System.currentTimeMillis()
                )
                repository.insertMessage(acceptanceSuccess)
            }
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            repository.deleteFriend(friendId)
        }
    }

    // Video Call launch logic (Permitted only for "FRIENDS")
    fun startVideoCall(friend: Friend) {
        if (friend.status == "FRIENDS") {
            _activeVideoCallFriend.value = friend
            _videoCallActive.value = true
        }
    }

    fun endVideoCall() {
        _videoCallActive.value = false
        _activeVideoCallFriend.value = null
    }

    private fun generateSimulatedReply(friend: Friend, userMsg: String, stickerName: String?): String {
        if (stickerName != null) {
            return listOf(
                "Oh my gosh, that sticker is so adorable! My heart is melting! 💖🥺",
                "Sticker bomb! Let me cuddle that! Here is a virtual squishy hug! 🧸💕",
                "Cute stickers define our aesthetic! 🌸✨ Let's collect them all!",
                "You have elite sticker taste! 🥰 Let me sticker back!"
            ).random()
        }

        val msg = userMsg.lowercase()
        return when {
            msg.contains("hello") || msg.contains("hi") || msg.contains("hey") -> {
                listOf(
                    "Hello sweet friend! What a gorgeous day to chat! 💖🌸",
                    "Hey there! Sending you a pocket full of sunshine! 🥰🌟",
                    "A wild friendly greeting! Peek-a-boo! 🐰🍪",
                    "Hi bestie! How's your open world exploration going?"
                ).random()
            }
            msg.contains("encrypt") || msg.contains("e2ee") || msg.contains("secure") || msg.contains("safe") -> {
                "Absolutely! Look at the top! Outgoing messages are encrypted by our proprietary LoveCrypt key before they hit the cloud! Tap our bubble locks to see it! 🗝️🔒"
            }
            msg.contains("video") || msg.contains("call") || msg.contains("camera") -> {
                "Ooh! Video calls are so much fun with overlays! Click the camera button next to my name and let's try the cute flower crowns! 🎥🌸"
            }
            msg.contains("love") || msg.contains("cute") -> {
                "Aww, you are incredibly sweet! Sending you absolute love, positive vibes, and a shower of dynamic hearts! 🥰💖"
            }
            msg.contains("sticker") -> {
                "I have a sticker drawer too! Tell me which one is your absolute favorite! 🐱💝"
            }
            else -> {
                listOf(
                    "That is so interesting! Tell me more, my friend! 🧁✨",
                    "Oh wow! You make my day super joyful and happy! 🌸🦋",
                    "Let's play and keep our chats completely safe! Did you turn on the high-security padlock? 🔒🗝️",
                    "Hehe, let's keep sending cozy stickers and laughing! 😄💘"
                ).random()
            }
        }
    }
}
