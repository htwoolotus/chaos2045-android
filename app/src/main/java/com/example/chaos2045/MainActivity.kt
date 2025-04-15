package com.example.chaos2045

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chaos2045.database.SharedContentDatabase
import com.example.chaos2045.ui.BakingScreen
import com.example.chaos2045.ui.ContentDetailScreen
import com.example.chaos2045.ui.SharedContentScreen
import com.example.chaos2045.ui.theme.Chaos2045Theme

class MainActivity : ComponentActivity() {
    private lateinit var database: SharedContentDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = SharedContentDatabase(this)

        setContent {
            Chaos2045Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "shared_content") {
                        composable("baking") {
                            BakingScreen(
                                onNavigateToSharedContent = {
                                    navController.navigate("shared_content")
                                }
                            )
                        }
                        composable("shared_content") {
                            SharedContentScreen(
                                database = database,
                                onNavigateToDetail = { contentId ->
                                    navController.navigate("content_detail/$contentId")
                                }
                            )
                        }
                        composable("content_detail/{contentId}") { backStackEntry ->
                            val contentId = backStackEntry.arguments?.getString("contentId")?.toLongOrNull()
                            if (contentId != null) {
                                ContentDetailScreen(
                                    contentId = contentId,
                                    database = database,
                                    onNavigateBack = { navController.popBackStack() },
                                    onContentDeleted = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            // 后台打印 intent.type 内容
            android.util.Log.d("SharedIntent", "Received intent type: ${intent?.type}")
            when (intent.type) {
                "text/plain" -> {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (sharedText != null) {
                        database.insertSharedContent(sharedText, "text")
                    }
                }
            }
        }
    }
}