package com.example.chaos2045.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.chaos2045.database.SharedContent
import com.example.chaos2045.database.SharedContentDatabase
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedContentScreen(
    database: SharedContentDatabase,
    onNavigateToDetail: (Long) -> Unit
) {
    var sharedContent by remember { mutableStateOf<List<SharedContent>>(emptyList()) }

    LaunchedEffect(Unit) {
        sharedContent = database.getAllSharedContent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shared Content") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (sharedContent.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No shared content yet")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = sharedContent,
                        key = { it.id }
                    ) { content ->
                        SwipeableContent(
                            content = content,
                            onDismiss = {
                                database.deleteSharedContent(content.id)
                                sharedContent = database.getAllSharedContent()
                            },
                            onClick = { onNavigateToDetail(content.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeableContent(
    content: SharedContent,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var isDismissed by remember { mutableStateOf(false) }

    if (!isDismissed) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Delete background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            // Content card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-200f, 0f)
                            if (offsetX < -150f) {
                                isDismissed = true
                                onDismiss()
                            }
                        }
                    },
                onClick = onClick
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = content.content,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Type: ${content.type}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Time: ${formatTimestamp(content.timestamp)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(timestamp)
        outputFormat.format(date)
    } catch (e: Exception) {
        timestamp
    }
} 