package com.lukuman.freechat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lukuman.freechat.data.model.ChatThread
import com.lukuman.freechat.data.model.UserProfile
import com.lukuman.freechat.ui.components.UserAvatar
import com.lukuman.freechat.ui.i18n.AppStrings
import com.lukuman.freechat.ui.theme.DeepBluePalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    strings: AppStrings,
    currentUser: UserProfile,
    chats: List<ChatThread>,
    onOpenChat: (String) -> Unit,
    onOpenFindUsers: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Scaffold(
        containerColor = DeepBluePalette.MidnightNavy,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBluePalette.DeepOcean,
                    titleContentColor = DeepBluePalette.IceInk
                ),
                title = {
                    Column {
                        Text(
                            text = strings.appName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.chatsTitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepBluePalette.SapphireSky
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenFindUsers) {
                        Icon(
                            imageVector = Icons.Rounded.PersonSearch,
                            contentDescription = strings.findUsersTitle,
                            tint = DeepBluePalette.IceInk
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable(onClick = onOpenProfile)
                    ) {
                        UserAvatar(
                            name = currentUser.displayName,
                            photoUrl = currentUser.photoUrl,
                            size = 36.dp
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenFindUsers,
                containerColor = DeepBluePalette.RoyalCobalt,
                contentColor = DeepBluePalette.IceInk,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PersonSearch,
                    contentDescription = strings.startNewChat
                )
            }
        }
    ) { innerPadding ->
        if (chats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(DeepBluePalette.AbyssSurface, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubbleOutline,
                            contentDescription = null,
                            tint = DeepBluePalette.SapphireSky,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = strings.noChatsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepBluePalette.IceInk,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.noChatsBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepBluePalette.SteelMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Button(
                        onClick = onOpenFindUsers,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepBluePalette.RoyalCobalt,
                            contentColor = DeepBluePalette.IceInk
                        )
                    ) {
                        Text(text = strings.startNewChat)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(chats, key = { it.chatId }) { thread ->
                    ChatThreadRow(
                        strings = strings,
                        currentUid = currentUser.uid,
                        thread = thread,
                        onClick = { onOpenChat(thread.chatId) }
                    )
                    HorizontalDivider(
                        color = DeepBluePalette.HairlineBorder.copy(alpha = 0.55f),
                        modifier = Modifier.padding(start = 78.dp, end = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatThreadRow(
    strings: AppStrings,
    currentUid: String,
    thread: ChatThread,
    onClick: () -> Unit
) {
    val peerName = thread.peerName(currentUid)
    val peerPhoto = thread.peerPhoto(currentUid)
    val preview = if (thread.lastMessageText.isBlank()) {
        strings.newChatPlaceholder
    } else if (thread.lastMessageSenderId == currentUid) {
        "${strings.youPrefix}${thread.lastMessageText}"
    } else {
        thread.lastMessageText
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            name = peerName,
            photoUrl = peerPhoto,
            size = 50.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = peerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepBluePalette.IceInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatChatTimestamp(thread.lastMessageAtMillis, strings),
                    style = MaterialTheme.typography.labelMedium,
                    color = DeepBluePalette.SapphireSky
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = preview,
                style = MaterialTheme.typography.bodyMedium,
                color = DeepBluePalette.SteelMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatChatTimestamp(millis: Long, strings: AppStrings): String {
    if (millis <= 0L) return ""
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = millis }

    val sameDay = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    if (sameDay) {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
    }

    now.add(Calendar.DAY_OF_YEAR, -1)
    val isYesterday = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    if (isYesterday) {
        return strings.yesterdayLabel
    }

    return SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(millis))
}
