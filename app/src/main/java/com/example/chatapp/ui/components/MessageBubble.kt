package com.example.chatapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.chatapp.data.model.Message

@Composable
fun MessageBubble(
    message: Message,
    isSender: Boolean,
    senderName: String? = null,
    onImageClick: () -> Unit
) {
    val bubbleColor = if (isSender) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val alignment = if (isSender) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        contentAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .widthIn(max = 300.dp),
                horizontalAlignment = if (isSender) Alignment.End else Alignment.Start
            ) {
                senderName?.let {
                    Text(
                        text = it,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (message.type == "text") {
                    Text(text = message.text)
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(message.imageUrl),
                        contentDescription = "Image",
                        modifier = Modifier
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick() },
                        contentScale = ContentScale.Crop
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = android.text.format.DateFormat.format("h:mm a", message.timestamp).toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isSender) {
                        Icon(
                            imageVector = when {
                                message.seen -> Icons.Default.DoneAll
                                message.delivered -> Icons.Default.DoneAll
                                else -> Icons.Default.Done
                            },
                            contentDescription = "Message Status",
                            modifier = Modifier.size(16.dp),
                            tint = if (message.seen) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
        }
    }
}
