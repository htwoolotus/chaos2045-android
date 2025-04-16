package com.example.chaos2045.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chaos2045.database.SharedContentEntity
import com.example.chaos2045.database.SharedContentDatabase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentDetailScreen(
    contentId: Long,
    database: SharedContentDatabase,
    onNavigateBack: () -> Unit,
    onContentDeleted: () -> Unit
) {
    var content by remember { mutableStateOf<SharedContentEntity?>(null) }

    LaunchedEffect(contentId) {
        //content = database.getSharedContentById(contentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Content Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = {
                        content?.let {
                            database.sharedContentDao().deleteContent(it.id)
                            onContentDeleted()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Content")
                }
            }
        }
    ) { paddingValues ->
        content?.let { sharedContent ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text(
                    text = sharedContent.content,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Type: ${sharedContent.type}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Time: ${formatTimestamp(sharedContent.timestamp)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("Content not found")
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val date = Date(timestamp)
    return dateFormat.format(date)
} 