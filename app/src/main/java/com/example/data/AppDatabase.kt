package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Friend::class, Message::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun friendDao(): FriendDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chat_with_love_db"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.friendDao(), database.messageDao())
                }
            }
        }

        suspend fun populateDatabase(friendDao: FriendDao, messageDao: MessageDao) {
            val defaultFriends = listOf(
                Friend(
                    id = "teddy_sweet",
                    name = "Teddy Bear 🧸",
                    avatar = "🧸",
                    bio = "Shy, sweet, and loves chocolate cookies! Send me a cozy heart sticker. 🍪✨",
                    status = "RECRUIT"
                ),
                Friend(
                    id = "bubble_kitt",
                    name = "Kitty Bubbles 🐱",
                    avatar = "🐱",
                    bio = "Professional backflipper! I sticker-bomb when I'm super happy. Let's send playful animations! 🐾",
                    status = "RECRUIT"
                ),
                Friend(
                    id = "pinky_fluf",
                    name = "Pinky Flamingo 🦩",
                    avatar = "🦩",
                    bio = "Loves bright pink hearts and sharing secret safe encrypted notes! Request video calls once we are close! 💞",
                    status = "PENDING_ACCEPT", // incoming request
                    isOnline = true
                ),
                Friend(
                    id = "cupcake_fairy",
                    name = "Cupcake Fairy 🧁",
                    avatar = "🧁",
                    bio = "Baking sweet frosted dreams and looking for local cookie loving besties! 😋🌸",
                    status = "PENDING_ACCEPT", // incoming request
                    isOnline = true
                ),
                Friend(
                    id = "marshmallow_cloud",
                    name = "Marshmallow Cloud ☁️",
                    avatar = "☁️",
                    bio = "Super fluffy bot living in the pink starry skies! Always online to cuddle up. 🌟☁️",
                    status = "PENDING_ACCEPT", // incoming request
                    isOnline = false
                ),
                Friend(
                    id = "pixel_heart",
                    name = "Pixel Heart 👾",
                    avatar = "👾",
                    bio = "Retro arcade love generator! Sending cyber stickers and high-scores! 🕹️💖",
                    status = "PENDING_ACCEPT", // incoming request
                    isOnline = true
                ),
                Friend(
                    id = "boba_bub",
                    name = "Boba Bubble 🧋",
                    avatar = "🧋",
                    bio = "Brewing sweet jasmine milk tea and searching for cute besties to share! 🧋✨",
                    status = "RECRUIT",
                    isOnline = true
                ),
                Friend(
                    id = "star_dust",
                    name = "Stardust Sparkle ✨",
                    avatar = "✨",
                    bio = "Glow-in-the-dark magical bot that loves secure end-to-end encryption! 🗝️🔒",
                    status = "RECRUIT",
                    isOnline = false
                ),
                Friend(
                    id = "heart_spak",
                    name = "Hearty Spark 💝",
                    avatar = "💝",
                    bio = "E2EE enthusiast! Safety first, love forever. Tap standard chat locks to verify your pink key! 🗝️🔒",
                    status = "RECRUIT"
                ),
                Friend(
                    id = "honey_bunny",
                    name = "Honey Bunny 🐰",
                    avatar = "🐰",
                    bio = "Hop Hop! Let's do a love video call together and try out the cute heart crown stickers! 👑🌸",
                    status = "FRIENDS" // prepopulated friends so they can unlock video calls immediately if they want
                )
            )
            friendDao.insertFriends(defaultFriends)

            // Some initial playful messages to show history
            messageDao.insertMessage(
                Message(
                    senderId = "honey_bunny",
                    receiverId = "me",
                    content = "Hey! Tap my profile and start our encrypted call! 🌸✨",
                    encryptedContent = null,
                    isEncrypted = false,
                    stickerName = null,
                    timestamp = System.currentTimeMillis() - 120000
                )
            )
            messageDao.insertMessage(
                Message(
                    senderId = "me",
                    receiverId = "honey_bunny",
                    content = "Oh, hi Honey Bunny! I'm trying out the sticker drawer!",
                    encryptedContent = null,
                    isEncrypted = false,
                    stickerName = null,
                    timestamp = System.currentTimeMillis() - 60000
                )
            )
            messageDao.insertMessage(
                Message(
                    senderId = "honey_bunny",
                    receiverId = "me",
                    content = "Send me some cute stickers right here!",
                    encryptedContent = null,
                    isEncrypted = false,
                    stickerName = "love_spark",
                    timestamp = System.currentTimeMillis() - 30000
                )
            )

            // Feed general channel messages
            messageDao.insertMessage(
                Message(
                    senderId = "pinky_fluf",
                    receiverId = "open_general",
                    content = "Hello Open World! This chatroom is super energetic and safe! 😘🎉",
                    encryptedContent = null,
                    isEncrypted = false,
                    stickerName = null,
                    timestamp = System.currentTimeMillis() - 600000,
                    channelId = "open_general"
                )
            )
            messageDao.insertMessage(
                Message(
                    senderId = "teddy_sweet",
                    receiverId = "open_general",
                    content = "Joining the party! Love you all! 🌸🧸",
                    encryptedContent = null,
                    isEncrypted = false,
                    stickerName = "hug_bear",
                    timestamp = System.currentTimeMillis() - 300000,
                    channelId = "open_general"
                )
            )
        }
    }
}
