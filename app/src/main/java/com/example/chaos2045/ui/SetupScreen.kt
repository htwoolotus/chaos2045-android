package com.example.chaos2045.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chaos2045.database.ForwardingConfigEntity
import com.example.chaos2045.database.SharedContentDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(database: SharedContentDatabase) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<ForwardingConfigEntity?>(null) }
    var forwardConfigs by remember { mutableStateOf(listOf<ForwardingConfigEntity>()) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            forwardConfigs = database.forwardingConfigDao().getAllConfigs()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Text("Add")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "App Settings",
                style = MaterialTheme.typography.headlineMedium
            )
            
            // Forward Config List
            forwardConfigs.forEach { config ->
                ForwardConfigCard(
                    config = config,
                    onEdit = { showEditDialog = config },
                    onDelete = {
                        scope.launch(Dispatchers.IO) {
                            database.forwardingConfigDao().deleteConfig(config)
                            forwardConfigs = database.forwardingConfigDao().getAllConfigs()
                        }
                    }
                )
            }
        }
    }
    
    if (showAddDialog) {
        ForwardConfigDialog(
            title = "Add Forward Config",
            onDismiss = { showAddDialog = false },
            onConfirm = { newConfig ->
                scope.launch(Dispatchers.IO) {
                    database.forwardingConfigDao().insertConfig(newConfig)
                    forwardConfigs = database.forwardingConfigDao().getAllConfigs()
                    showAddDialog = false
                }
            }
        )
    }
    
    showEditDialog?.let { config ->
        ForwardConfigDialog(
            title = "Edit Forward Config",
            initialConfig = config,
            onDismiss = { showEditDialog = null },
            onConfirm = { updatedConfig ->
                scope.launch(Dispatchers.IO) {
                    database.forwardingConfigDao().deleteConfig(config)
                    database.forwardingConfigDao().insertConfig(updatedConfig)
                    forwardConfigs = database.forwardingConfigDao().getAllConfigs()
                    showEditDialog = null
                }
            }
        )
    }
}

@Composable
fun ForwardConfigCard(
    config: ForwardingConfigEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "URL: ${config.apiUrl}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Token: ${config.token}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Match Text: ${config.matchString}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardConfigDialog(
    title: String,
    initialConfig: ForwardingConfigEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (ForwardingConfigEntity) -> Unit
) {
    var url by remember { mutableStateOf(initialConfig?.apiUrl ?: "") }
    var token by remember { mutableStateOf(initialConfig?.token ?: "") }
    var matchText by remember { mutableStateOf(initialConfig?.matchString ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Token") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = matchText,
                    onValueChange = { matchText = it },
                    label = { Text("Match Text") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(ForwardingConfigEntity(
                        id = initialConfig?.id ?: 0,
                        apiUrl = url,
                        token = token,
                        matchString = matchText
                    ))
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 