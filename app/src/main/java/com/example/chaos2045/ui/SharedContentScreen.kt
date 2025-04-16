package com.example.chaos2045.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.chaos2045.database.SharedContentEntity
import com.example.chaos2045.database.SharedContentDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

//import com.example.chaos2045.database.SharedContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedContentScreen(
    database: SharedContentDatabase,
    onNavigateToDetail: (Long) -> Unit
) {
    var sharedContent by remember { mutableStateOf<List<SharedContentEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            sharedContent = database.sharedContentDao().getAllContent()
        }
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
                                //scope.launch(Dispatchers.IO) {
                                    //database.sharedContentDao().deleteContent(content.id)
                                //    sharedContent = database.sharedContentDao().getAllContent()
                                //}
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
    content: SharedContentEntity,
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
                    .padding(8.dp)
                    .clickable {
                        isDismissed = true
                        onDismiss()
                    },
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
                                //isDismissed = true
                                //onDismiss()
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

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return format.format(date)
} 