package com.example.chatapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.chatapp.data.model.ChatItem
import com.example.chatapp.utils.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListItem(
    chatItem: ChatItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(chatItem.profileImage),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                if (chatItem.isGroup) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Group Chat",
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                } else if (chatItem.isOnline) {
                    Badge(modifier = Modifier.align(Alignment.BottomEnd)) {}
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = chatItem.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = chatItem.lastMessage,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = TimeUtils.formatTimestamp(chatItem.timestamp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (chatItem.unreadCount > 0) {
                    Badge {
                        Text(text = chatItem.unreadCount.toString())
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatListItemPreview() {
    ChatListItem(
        chatItem = ChatItem(
            id = "1",
            name = "John Doe",
            lastMessage = "Hey, how are you?",
            unreadCount = 2,
            isOnline = true,
            isGroup = false,
            profileImage = "",
            timestamp = System.currentTimeMillis()
        ),
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun GroupChatListItemPreview() {
    ChatListItem(
        chatItem = ChatItem(
            id = "2",
            name = "Android Devs",
            lastMessage = "New message!",
            unreadCount = 5,
            isGroup = true,
            isOnline = false,
            profileImage = "",
            timestamp = System.currentTimeMillis()
        ),
        onClick = {}
    )
}
