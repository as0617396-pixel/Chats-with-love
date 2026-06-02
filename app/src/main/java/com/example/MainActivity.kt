package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.CryptoHelper
import com.example.data.Friend
import com.example.data.Message
import com.example.ui.ChatViewModel
import com.example.ui.ComposableSticker
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    // Real Permission check callback for playful full-video simulation
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Camera access granted! Video Call filter enabled! 🎥✨", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Camera permission denied. Simulation running in avatar mode!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    MainLayout(
                        viewModel = viewModel,
                        paddingValues = innerPadding,
                        onRequestCameraPermission = {
                            requestCameraPermission(Manifest.permission.CAMERA)
                        }
                    )
                }
            }
        }
    }

    private fun requestCameraPermission(permission: String) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(permission)
        }
    }
}

// Particle class for floating animation effect
data class EmojiParticle(
    val id: Int,
    val emoji: String,
    val startX: Float, // percentage of screen width
    val scale: Float,
    val duration: Int,
    val delayMs: Int
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainLayout(
    viewModel: ChatViewModel,
    paddingValues: PaddingValues,
    onRequestCameraPermission: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentTab by viewModel.currentTab.collectAsState()
    val isEncryptionEnabled by viewModel.isEncryptionEnabled.collectAsState()
    val activeChatFriend by viewModel.activeChatFriend.collectAsState()
    val activeChannelId by viewModel.activeChannelId.collectAsState()

    // Floating particles state manager
    val emojiShowerTriggerCount by viewModel.emojiShowerTriggers.collectAsState()
    val emojiShowerType by viewModel.emojiShowerType.collectAsState()
    val particlesList = remember { mutableStateListOf<EmojiParticle>() }

    // On trigger count change, populate standard shower particles
    LaunchedEffect(emojiShowerTriggerCount) {
        if (emojiShowerTriggerCount > 0) {
            particlesList.clear()
            repeat(30) { index ->
                particlesList.add(
                    EmojiParticle(
                        id = index,
                        emoji = emojiShowerType,
                        startX = Random.nextFloat(),
                        scale = Random.nextFloat() * 1.5f + 0.6f,
                        duration = Random.nextInt(2500, 4500),
                        delayMs = Random.nextInt(0, 1000)
                    )
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // App Core Layout
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            MainAppBar(
                viewModel = viewModel,
                isEncryptionEnabled = isEncryptionEnabled,
                onToggleEncryption = { viewModel.toggleEncryption(it) }
            )

            // Content Area based on Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    "WORLD" -> WorldChatScreen(viewModel)
                    "FRIENDS" -> FriendsListScreen(viewModel)
                    "CHATTING_FRIEND" -> FriendPrivateChatScreen(viewModel)
                    "DISCOVER" -> DiscoverDiscoveryScreen(viewModel)
                    "VAULT" -> EncryptionVaultScreen(viewModel)
                }
            }

            // Bottom Navigation (Respect safe gesture bar)
            BottomNavBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }

        // Overlay for Floating Emojis Rain
        FloatingEmojiCanvas(particles = particlesList)

        // Overlay for Interactive Love Video Call (Triggered when videoCallActive is true)
        val videoCallActive by viewModel.videoCallActive.collectAsState()
        val activeVideoFriend by viewModel.activeVideoCallFriend.collectAsState()

        if (videoCallActive && activeVideoFriend != null) {
            VideoCallModal(
                viewModel = viewModel,
                friend = activeVideoFriend!!,
                onRequestCameraPermission = onRequestCameraPermission,
                onEndCall = { viewModel.endVideoCall() }
            )
        }
    }
}

@Composable
fun MainAppBar(
    viewModel: ChatViewModel,
    isEncryptionEnabled: Boolean,
    onToggleEncryption: (Boolean) -> Unit
) {
    val myProfileName by viewModel.myProfileName.collectAsState()
    var showProfileDialog by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Playful App Branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showProfileDialog = true }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💖", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Chat with Love",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = myProfileName,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Encryption Mode Switch Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isEncryptionEnabled) Color(0xFFFFE0E5) else Color(0xFFF1F1F1))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .clickable { onToggleEncryption(!isEncryptionEnabled) }
                        .testTag("e2ee_switch_row")
                ) {
                    Icon(
                        imageVector = if (isEncryptionEnabled) Icons.Filled.Lock else Icons.Outlined.LockOpen,
                        contentDescription = "E2EE encryption Status",
                        tint = if (isEncryptionEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isEncryptionEnabled) "E2EE ON" else "E2EE OFF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEncryptionEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }

    // Profile customization dialog
    if (showProfileDialog) {
        var tempName by remember { mutableStateOf(myProfileName) }
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Custom Sweet Identity 🧸", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Change your open world display nickname. Make it sweet!", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Your Nickname") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfileName(tempName)
                        showProfileDialog = false
                    },
                    modifier = Modifier.testTag("profile_save")
                ) {
                    Text("Save 💖")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BottomNavBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        tonalElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("app_bottom_nav_bar")
    ) {
        NavigationBarItem(
            selected = currentTab == "WORLD",
            onClick = { onTabSelected("WORLD") },
            label = { Text("Open World") },
            icon = { Icon(Icons.Filled.Public, contentDescription = "Open World") },
            modifier = Modifier.testTag("nav_world")
        )
        NavigationBarItem(
            selected = currentTab == "FRIENDS" || currentTab == "CHATTING_FRIEND",
            onClick = { onTabSelected("FRIENDS") },
            label = { Text("Besties") },
            icon = { Icon(Icons.Filled.Favorite, contentDescription = "Besties") },
            modifier = Modifier.testTag("nav_friends")
        )
        NavigationBarItem(
            selected = currentTab == "DISCOVER",
            onClick = { onTabSelected("DISCOVER") },
            label = { Text("Match Deck") },
            icon = { Icon(Icons.Filled.CrueltyFree, contentDescription = "Discover Match") },
            modifier = Modifier.testTag("nav_discover")
        )
        NavigationBarItem(
            selected = currentTab == "VAULT",
            onClick = { onTabSelected("VAULT") },
            label = { Text("Safe Keys") },
            icon = { Icon(Icons.Filled.Key, contentDescription = "Security Vault") },
            modifier = Modifier.testTag("nav_vault")
        )
    }
}

// ------------------ TAB 1: OPEN WORLD CHAT ------------------
@Composable
fun WorldChatScreen(viewModel: ChatViewModel) {
    val channelId by viewModel.activeChannelId.collectAsState()
    val messages by viewModel.channelMessages.collectAsState(initial = emptyList())
    val isEncryptionEnabled by viewModel.isEncryptionEnabled.collectAsState()

    var textState by remember { mutableStateOf("") }
    var showStickers by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto scroll when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Channel Sub-Tabs
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
        ) {
            val rooms = listOf(
                "open_general" to "🌎 General Lounge",
                "love_sparks" to "⚡ Love Sparks",
                "secret_spices" to "🧁 Secret Dessert"
            )
            rooms.forEach { (id, label) ->
                val selected = channelId == id
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.selectActiveChannel(id) },
                    label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("room_chip_$id")
                )
            }
        }

        // Message List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFFFF0F2))))
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🧁", fontSize = 54.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "The Open world room is clean and quiet.\nSend the first heart-coded secret!",
                            textAlign = TextAlign.Center, color = Color.Gray, fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages) { msg ->
                        MessageChatBubble(
                            message = msg,
                            myProfileName = viewModel.myProfileName.value,
                            viewModel = viewModel
                        )
                    }
                }
            }

            // Quick emoji shower action buttons (floating on chat window)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val loveIcons = listOf("💖", "😻", "🎈", "🍫", "✨")
                    loveIcons.forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.85f))
                                .border(1.dp, Color(0xFFFFB2C1), CircleShape)
                                .clickable { viewModel.triggerEmojiShower(i) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(i, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        // Input bottom bar
        ChatInputBar(
            text = textState,
            onTextChanged = { textState = it },
            onSendMessage = {
                viewModel.sendChannelMessage(textState)
                textState = ""
            },
            isEncryptionEnabled = isEncryptionEnabled,
            showStickers = showStickers,
            onToggleStickers = { showStickers = !showStickers },
            onStickerSent = { sticker ->
                viewModel.sendChannelMessage("", stickerName = sticker)
                showStickers = false
            }
        )
    }
}

// ------------------ TAB 2 & 3: PRIVATE CHAT / BESTIES LIST ------------------
@Composable
fun LiveAvatarBadge(avatar: String, isOnline: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier.size(54.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .alpha(pulseAlpha)
                    .background(Color(0xFF00FFCC).copy(alpha = 0.2f), shape = CircleShape)
                    .border(
                        width = 2.dp,
                        color = Color(0xFF00FFCC),
                        shape = CircleShape
                    )
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(avatar, fontSize = 20.sp)
        }
    }
}

@Composable
fun FriendsListScreen(viewModel: ChatViewModel) {
    val friends by viewModel.friends.collectAsState()
    val activeFriends = friends.filter { it.status == "FRIENDS" }
    val invites = friends.filter { it.status == "PENDING_ACCEPT" }

    var showOnlineOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF9FA))
            .padding(16.dp)
    ) {
        // App header with Spawn Bots action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Sweet Hub 🌸",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Manage connections & active statuses",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = { viewModel.spawnRandomFriendRequestBot() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(34.dp).testTag("spawn_bot_button")
            ) {
                Text("Spawn Bot Request 🤖💞", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
            }
        }

        // Pending Invites Section (Send, Accept, Decline system)
        if (invites.isNotEmpty()) {
            Text(
                text = "Chat Requests 💌 (${invites.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                items(invites) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F3)),
                        modifier = Modifier.fillMaxWidth().testTag("invite_card_${item.id}"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Avatar with live pulsing ring
                            LiveAvatarBadge(avatar = item.avatar, isOnline = item.isOnline)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(item.bio, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.acceptFriendRequest(item.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp).testTag("accept_${item.id}")
                                ) {
                                    Text("Accept 🥰", fontSize = 10.sp, color = Color.White)
                                }
                                Button(
                                    onClick = { viewModel.declineFriendRequest(item.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp).testTag("decline_${item.id}")
                                ) {
                                    Text("Decline 💔", fontSize = 10.sp, color = Color(0xFFFF1744))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Active Besties List Filter + Toggle Container
        Text(
            text = "Active Besties 💝 (E2EE Unlocked)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Filter and Search Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search besties...", fontSize = 11.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("search_besties"),
                textStyle = TextStyle(fontSize = 12.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFFFFC0CB)
                )
            )

            // Online Only Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (showOnlineOnly) Color(0xFFD0F8E4) else Color(0xFFF1F1F1))
                    .clickable { showOnlineOnly = !showOnlineOnly }
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .testTag("filter_online_only")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (showOnlineOnly) Color(0xFF00E676) else Color.Gray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Online Only 🟢",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showOnlineOnly) Color(0xFF2E7D32) else Color.DarkGray
                    )
                }
            }
        }

        val filteredFriends = activeFriends.filter { friend ->
            val matchesSearch = friend.name.contains(searchQuery, ignoreCase = true) || friend.bio.contains(searchQuery, ignoreCase = true)
            val matchesOnline = !showOnlineOnly || friend.isOnline
            matchesSearch && matchesOnline
        }

        if (filteredFriends.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text("🐱🐾", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No sweet besties matching current filters!\nSwipe more matches in \"Match Deck\" tab or adjust filters. 💓",
                        textAlign = TextAlign.Center, fontSize = 12.sp, color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredFriends) { friend ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectActiveFriend(friend) }
                            .testTag("friend_card_${friend.id}"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Avatar Badge with pulsing ring
                            LiveAvatarBadge(avatar = friend.avatar, isOnline = friend.isOnline)

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { viewModel.toggleFriendOnlineStatus(friend.id) } // Click name/dot to toggle status!
                                ) {
                                    Text(friend.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(if (friend.isOnline) Color(0xFF00FFCC) else Color.LightGray, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (friend.isOnline) "online" else "offline",
                                        fontSize = 10.sp,
                                        color = if (friend.isOnline) Color(0xFF2E7D32) else Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = friend.bio,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Private Chat and Video Call shortcuts
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { viewModel.selectActiveFriend(friend) },
                                    modifier = Modifier.testTag("chat_shortcut_${friend.id}")
                                ) {
                                    Icon(Icons.Outlined.Chat, contentDescription = "Direct Chat Chat", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(
                                    onClick = { viewModel.startVideoCall(friend) },
                                    modifier = Modifier.testTag("videocall_shortcut_${friend.id}")
                                ) {
                                    Icon(Icons.Filled.VideoCall, contentDescription = "Video Call", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendPrivateChatScreen(viewModel: ChatViewModel) {
    val friend by viewModel.activeChatFriend.collectAsState()
    val messages by viewModel.privateMessages.collectAsState(initial = emptyList())
    val isEncryptionEnabled by viewModel.isEncryptionEnabled.collectAsState()
    val isFriendTyping by viewModel.isFriendTyping.collectAsState()

    if (friend == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Please select a Friend to chat.")
        }
        return
    }

    var textState by remember { mutableStateOf("") }
    var showStickers by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Friend mini action bar
        Surface(tonalElevation = 4.dp, color = Color.White) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                IconButton(onClick = { viewModel.selectActiveFriend(null); viewModel.selectTab("FRIENDS") }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back back friends")
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(friend!!.avatar, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(friend!!.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("🔒 E2EE Direct Connection Secured", fontSize = 10.sp, color = Color(0xFF2E7D32))
                }

                // Video Call Launcher
                Button(
                    onClick = { viewModel.startVideoCall(friend!!) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp).testTag("video_call_launcher")
                ) {
                    Icon(Icons.Filled.VideoCall, contentDescription = "Call call", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call 🎥", fontSize = 11.sp)
                }
            }
        }

        // Messages area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFFFF0F2))))
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(messages) { msg ->
                    MessageChatBubble(
                        message = msg,
                        myProfileName = viewModel.myProfileName.value,
                        viewModel = viewModel
                    )
                }
            }

            // High priority floating flower shower shortcut
            IconButton(
                onClick = { viewModel.triggerEmojiShower("🌸") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFFFB2C1), CircleShape)
            ) {
                Text("🌸", fontSize = 18.sp)
            }
        }

        // Snapchat-like live status avatar peeking in chat
        SnapchatLiveIndicator(
            friend = friend!!,
            isTyping = isFriendTyping,
            onEmojiShower = { emoji -> viewModel.triggerEmojiShower(emoji) }
        )

        // Input row
        ChatInputBar(
            text = textState,
            onTextChanged = { textState = it },
            onSendMessage = {
                viewModel.sendPrivateMessage(textState)
                textState = ""
            },
            isEncryptionEnabled = isEncryptionEnabled,
            showStickers = showStickers,
            onToggleStickers = { showStickers = !showStickers },
            onStickerSent = { sticker ->
                viewModel.sendPrivateMessage("", stickerName = sticker)
                showStickers = false
            }
        )
    }
}

@Composable
fun MessageChatBubble(
    message: Message,
    myProfileName: String,
    viewModel: ChatViewModel
) {
    val isMe = message.senderId == "me"
    val decryptedMessagesTemp by viewModel.decryptedMessagesTemp.collectAsState()
    val isFriendRequestOrPrepopulate = message.senderId == "system"

    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primary else Color.White
    val contentColor = if (isMe) Color.White else Color.Black
    val timeLabel = remember(message.timestamp) {
        val date = java.util.Date(message.timestamp)
        val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        format.format(date)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("message_bubble_${message.id}"),
        contentAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!isMe) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🐱", fontSize = 16.sp) // Fallback friendly preview
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                // Name label
                Text(
                    text = if (isMe) myProfileName else message.senderId,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )

                Surface(
                    shape = RoundedCornerShape(
                        topStart = if (isMe) 16.dp else 4.dp,
                        topEnd = if (isMe) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    color = bubbleColor,
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // If E2EE cipher status exists
                        if (message.isEncrypted) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isMe) Color(0x30FFFFFF) else Color(0x10FF0000))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Encrypted Message Badge",
                                    tint = if (isMe) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AES-256 E2E SECURE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) Color.White else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Message text or sticker card
                        if (message.stickerName != null) {
                            ComposableSticker(name = message.stickerName, modifier = Modifier.padding(vertical = 4.dp))
                        } else {
                            val displayText = if (message.isEncrypted) {
                                decryptedMessagesTemp[message.id] ?: "🗝️ tap to securely decode:\n${message.content}"
                            } else {
                                message.content
                            }
                            Text(
                                text = displayText,
                                fontSize = 14.sp,
                                color = contentColor,
                                modifier = Modifier.clickable {
                                    if (message.isEncrypted && decryptedMessagesTemp[message.id] == null) {
                                        viewModel.interactivelyDecryptMessage(message)
                                    }
                                }
                            )
                        }

                        // Cryptographic proof tooltip helpers
                        if (message.isEncrypted && decryptedMessagesTemp[message.id] == null) {
                            Text(
                                text = "Hex cipher: ${message.encryptedContent ?: "0x98f...🔒"}",
                                fontSize = 8.sp,
                                color = if (isMe) Color.LightGray else Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Time label
                Text(
                    text = timeLabel,
                    fontSize = 8.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            if (isMe) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧁", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    isEncryptionEnabled: Boolean,
    showStickers: Boolean,
    onToggleStickers: () -> Unit,
    onStickerSent: (String) -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Sticker Grid Selection Drawer
            AnimatedVisibility(
                visible = showStickers,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF0F3))
                        .padding(16.dp)
                ) {
                    Text(
                        "Adorable Sticker Tray 🧸🌸",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val stickers = listOf("love_spark", "hug_bear", "laugh_heart", "music_love", "sweet_bubble")
                        stickers.forEach { st ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onStickerSent(st) }
                                    .testTag("sticker_select_$st")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(6.dp)
                                ) {
                                    val icon = when (st) {
                                        "love_spark" -> "💖"
                                        "hug_bear" -> "🧸"
                                        "laugh_heart" -> "😂"
                                        "music_love" -> "🎵"
                                        "sweet_bubble" -> "💬"
                                        else -> "🌸"
                                    }
                                    Text(icon, fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(st.substringBefore("_").capitalize(), fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // Input Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Sticker Launcher
                IconButton(onClick = onToggleStickers) {
                    Icon(
                        imageVector = Icons.Outlined.SentimentSatisfied,
                        contentDescription = "Open Stickers Tray Drawer",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Chat text input with Encryption indicators
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    placeholder = {
                        Text(
                            text = if (isEncryptionEnabled) "💬 Type E2EE encrypted secret..." else "💬 Type playful chat note...",
                            fontSize = 13.sp
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFFFC0CB)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text_field"),
                    singleLine = true,
                    leadingIcon = {
                        if (isEncryptionEnabled) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Security encrypted typing",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Send Button
                FloatingActionButton(
                    onClick = onSendMessage,
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("send_button")
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ------------------ TAB 3: DISCOVERY MATCH DECK ------------------
@Composable
fun DiscoverDiscoveryScreen(viewModel: ChatViewModel) {
    val friends by viewModel.friends.collectAsState()
    // Show recruits of status RECRUIT or SENT
    val recruits = friends.filter { it.status == "RECRUIT" || it.status == "SENT" }

    var showAddRecruitModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF7F9))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Open World Deck 🌎✨",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = { showAddRecruitModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Spawn Recruit 🧸", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (recruits.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎆🍿", fontSize = 54.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Wow! You matched with every open world traveler today!\nSpawn a custom traveler with the button above! 💓",
                        textAlign = TextAlign.Center, fontSize = 13.sp, color = Color.Gray
                    )
                }
            }
        } else {
            // Tinder Swipe Matcher Deck visual representation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val topRecruit = recruits.first()
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(340.dp)
                        .border(1.dp, Color(0xFFFFD1DC), RoundedCornerShape(24.dp))
                        .testTag("recruit_card_${topRecruit.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Avatar Header Bubble
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(topRecruit.avatar, fontSize = 52.sp)
                        }

                        // Name & Bio description
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = topRecruit.name,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = topRecruit.bio,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                lineHeight = 18.sp,
                                maxLines = 3,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        // Matching actions status
                        if (topRecruit.status == "SENT") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE2F4E5))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Request Sent! Pending acceptance... 🕒💞", fontSize = 11.sp, color = Color(0xFF2E7D32))
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { viewModel.removeFriend(topRecruit.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F1F1)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Skip 💔", color = Color.DarkGray, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = { viewModel.sendFriendRequestToRecruit(topRecruit.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("swipe_request_${topRecruit.id}")
                                ) {
                                    Text("Connect 💌", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal to spawn customizable characters
    if (showAddRecruitModal) {
        var newName by remember { mutableStateOf("") }
        var newAvatar by remember { mutableStateOf("🦄") }
        var newBio by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddRecruitModal = false },
            title = { Text("Spawn Custom Online Friend! 👾", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Define an interactive friend that you can request chat and video permissions with!", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Friend Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("spawn_name")
                    )

                    // Avatar Emoji list helper
                    Text("Select Buddy Emoji Avatar", fontSize = 11.sp, color = Color.Gray)
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val emojiList = listOf("🦄", "🦁", "🦊", "🐼", "🐨", "🧁", "🐥")
                        emojiList.forEach { em ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (newAvatar == em) MaterialTheme.colorScheme.primaryContainer else Color(0x10000000))
                                    .clickable { newAvatar = em },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(em, fontSize = 18.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newBio,
                        onValueChange = { newBio = it },
                        label = { Text("Personality Profile Bio") },
                        modifier = Modifier.fillMaxWidth().testTag("spawn_bio")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.recruitNewFriend(newName, newAvatar, newBio)
                        }
                        showAddRecruitModal = false
                    },
                    modifier = Modifier.testTag("spawn_save_button")
                ) {
                    Text("Spawn 💜")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRecruitModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ------------------ TAB 4: CRYPTO SECURITY KEYS VAULT ------------------
@Composable
fun EncryptionVaultScreen(viewModel: ChatViewModel) {
    var rawTextIn by remember { mutableStateOf("I love you, let's meet!") }
    val isEncryptionEnabled by viewModel.isEncryptionEnabled.collectAsState()

    // Test E2EE dynamic encryption details
    val demoPayload = remember(rawTextIn) {
        CryptoHelper.encrypt(rawTextIn, "love_keys_sandbox")
    }

    val demoDecrypted = remember(demoPayload) {
        CryptoHelper.decrypt(demoPayload.cipherText, "love_keys_sandbox")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF9FA))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "My Cryptographic E2EE Hub 🔐",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Every chat has End-to-End Encryption unlocked automatically. " +
                        "When typing, keys are securely derived locally. Not even are the database servers " +
                        "able to inspect your love-themed notes! Perfect Forward Secrecy is ACTIVE! ✅",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        item {
            Text(
                "E2EE Cipher Key Sandbox 🔍",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Input test secret to encrypt:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = rawTextIn,
                        onValueChange = { rawTextIn = it },
                        modifier = Modifier.fillMaxWidth().testTag("crypto_sandbox_input")
                    )

                    Divider(color = Color(0xFFFFB2C1))

                    Text("Active Cipher Key Name:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(
                        text = demoPayload.sharedKeyLabel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Text("Hex Encryption Proof (Saved to remote database):", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF0F5))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = demoPayload.hexProof,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text("Raw Base64 Encoded Ciphertext (Secret Message):", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE6E6FA))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = demoPayload.cipherText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF4B0082)
                        )
                    }

                    Text("Localized Decrypted result using secure key:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Active active", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = demoDecrypted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}

// ------------------ OVERLAY: FLOATING EMOJI CANVAS ANIMATION ------------------
@Composable
fun FloatingEmojiCanvas(particles: List<EmojiParticle>) {
    if (particles.isEmpty()) return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = maxWidth.value
        val height = maxHeight.value

        particles.forEach { particle ->
            // Floating up visual animation sequence using infinite dynamic transition offsets
            val infiniteTransition = rememberInfiniteTransition(label = "FloatAnim")
            val elapsedState = remember { Animatable(0f) }

            LaunchedEffect(particle.id) {
                delay(particle.delayMs.toLong())
                elapsedState.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = particle.duration,
                        easing = LinearEasing
                    )
                )
            }

            if (elapsedState.value > 0f && elapsedState.value < 1f) {
                // Wave horizontally as it floats upward
                val horizontalOscillation = sin(elapsedState.value * 12f) * 40f
                val currentX = (particle.startX * width * 3f) + horizontalOscillation
                val currentY = (1f - elapsedState.value) * height * 1.2f

                Box(
                    modifier = Modifier
                        .offset(x = currentX.dp, y = currentY.dp)
                        .alpha(1f - elapsedState.value)
                ) {
                    Text(
                        text = particle.emoji,
                        fontSize = (24 * particle.scale).sp
                    )
                }
            }
        }
    }
}

// ------------------ OVERLAY: INTERACTIVE LOVE VIDEO CALL MODAL ------------------
@Composable
fun VideoCallModal(
    viewModel: ChatViewModel,
    friend: Friend,
    onRequestCameraPermission: () -> Unit,
    onEndCall: () -> Unit
) {
    val sparklesFilter by viewModel.videoCallSparkles.collectAsState()
    val micMuted by viewModel.micMuted.collectAsState()
    val cameraMuted by viewModel.cameraMuted.collectAsState()

    var secondsElapsed by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // Automatically request active permission on call launch for fully functional camera handling
        onRequestCameraPermission()
        while (true) {
            delay(1000)
            secondsElapsed++
        }
    }

    val callTimeFmt = remember(secondsElapsed) {
        val m = secondsElapsed / 60
        val s = secondsElapsed % 60
        String.format("%02d:%02d", m, s)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_call_modal")
    ) {
        // SIMULATED CAMERA STREAM SCREEN FEED & FLOATING BUBBLES
        if (!cameraMuted) {
            // Camera simulator background gradients & visual sparkles
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFFFB2C1),
                                Color(0xFFC783FF),
                                Color(0xFF9C4153).copy(alpha = 0.8f)
                            )
                        )
                    )
            ) {
                // Background bubbles floating effect representation
                SimulatedFloatingWebcamFeed()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF201A1B)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷❌", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Camera is Turned Off", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }

        // REMOTE FRIEND PIC-IN-PIC WINDOW (PIP overlay in top corner)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 20.dp)
                .size(width = 110.dp, height = 150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .border(2.dp, Color(0xFFFF4B72), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(friend.avatar, fontSize = 44.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(friend.name.take(10), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("Remote Stream", fontSize = 8.sp, color = Color.Gray)
            }
        }

        // ACTIVE DYNAMIC OVERLAY FILTERS ON STREAM (Overlaid based on sparklesFilter selection)
        if (!cameraMuted && sparklesFilter != "None") {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (sparklesFilter) {
                        "Sparkles ✨" -> {
                            Text("✨💖🔮💖✨", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("✨", fontSize = 60.sp)
                        }
                        "Flower Crown 🌸" -> {
                            Text("🌸👑🌺👑🌸", fontSize = 42.sp)
                        }
                        "Cute Kitty 🐱" -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                                Text("🐱🐾", fontSize = 38.sp)
                                Text("🐾🐱", fontSize = 38.sp)
                            }
                            Spacer(modifier = Modifier.height(100.dp))
                            Text("😸😽", fontSize = 28.sp)
                        }
                        "Blushing Cheeks 🥰" -> {
                            Text("🥰", fontSize = 72.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(60.dp)) {
                                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Red.copy(alpha = 0.35f)))
                                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Red.copy(alpha = 0.35f)))
                            }
                        }
                    }
                }
            }
        }

        // CALL DETAILS / TIMER TOP BAR
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text("E2EE LOVE CALL", fontSize = 11.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = callTimeFmt,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // CONTROLS AND PLAYFUL DYNAMIC FILTER SELECTOR BAR (Bottom aligned)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
                .padding(bottom = 30.dp, start = 16.dp, end = 16.dp)
        ) {
            // Real Face Filter Selector
            Text(
                "Tap Playful Face Filter Overlay: 🤩🎭",
                color = Color.White,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                val filters = listOf("None", "Sparkles ✨", "Flower Crown 🌸", "Cute Kitty 🐱", "Blushing Cheeks 🥰")
                filters.forEach { filterOpt ->
                    val isSel = filterOpt == sparklesFilter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f))
                            .clickable { viewModel.setVideoFilter(filterOpt) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filterOpt.substringBefore(" "),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Mic, Camera toggle, and End Call control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic State
                FloatingActionButton(
                    onClick = { viewModel.toggleMic() },
                    shape = CircleShape,
                    containerColor = if (micMuted) Color.Red else Color.White.copy(alpha = 0.3f),
                    contentColor = Color.White,
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = if (micMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = "Mute Microphone"
                    )
                }

                // Disconnect End Call Button
                FloatingActionButton(
                    onClick = onEndCall,
                    shape = CircleShape,
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White,
                    modifier = Modifier.size(66.dp).testTag("end_video_call")
                ) {
                    Icon(Icons.Filled.CallEnd, contentDescription = "End Video Call", modifier = Modifier.size(28.dp))
                }

                // Camera Toggle State
                FloatingActionButton(
                    onClick = { viewModel.toggleCamera() },
                    shape = CircleShape,
                    containerColor = if (cameraMuted) Color.Red else Color.White.copy(alpha = 0.3f),
                    contentColor = Color.White,
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = if (cameraMuted) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                        contentDescription = "Toggle Camera Streams"
                    )
                }
            }
        }
    }
}

@Composable
fun SimulatedFloatingWebcamFeed() {
    val animProgress = rememberInfiniteTransition(label = "WebcamFlow")
    val waveOffset by animProgress.animateFloat(
        initialValue = 0f,
        targetValue = Math.PI.toFloat() * 2,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OffsetWave"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Simulated pixel trackers and romantic particles inside live feed
        repeat(12) { i ->
            val phase = i * (Math.PI / 6)
            val dx = width / 2 + sin(waveOffset + phase).getFloat() * (width / 3)
            val dy = height / 2 + cos(waveOffset * 1.5 + phase).getFloat() * (height / 4)
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = 15f + (i * 2),
                center = Offset(dx, dy)
            )

            // Dynamic love particles
            drawCircle(
                color = Color(0xFFFF69B4).copy(alpha = 0.3f),
                radius = 8f,
                center = Offset(dx + 20, dy - 20)
            )
        }
    }
}

// Float helper string converter
private fun Double.getFloat(): Float = this.toFloat()

@Composable
fun SnapchatLiveIndicator(
    friend: Friend,
    isTyping: Boolean,
    onEmojiShower: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("snapchat_live_indicator"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Live Avatar with pulsing neon green glowing Snapchat ring
                Box(contentAlignment = Alignment.Center) {
                    // Pulse ring
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .alpha(pulseAlpha)
                            .background(
                                color = if (isTyping) Color(0xFFFF4B72).copy(alpha = 0.35f) else Color(0xFF00FFCC).copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = if (isTyping) Color(0xFFFF4B72) else Color(0xFF00FFCC),
                                shape = CircleShape
                            )
                    )

                    // Actual Avatar inside the ring
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(friend.avatar, fontSize = 16.sp)
                    }

                    // Little green/pink badge dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isTyping) Color(0xFFFF1493) else Color(0xFF00E676),
                                shape = CircleShape
                            )
                            .border(1.5.dp, Color.White, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Text status
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = friend.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isTyping) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isTyping) "TYPING 💬" else "IN CHAT 🟢",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTyping) Color(0xFFFF1744) else Color(0xFF2E7D32)
                            )
                        }
                    }

                    if (isTyping) {
                        Text(
                            text = "is typing a sweet emoji message...",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            text = "Live peering screen! Send a quick emoji wave below 👇",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Quick interaction waves: tap to send instant sticker/emoji particles!
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("💖", "😻", "⚡").forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF0F3))
                            .clickable { onEmojiShower(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
