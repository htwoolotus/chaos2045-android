package com.example.chaos2045

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
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
                "image/jpeg" -> {
                    val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    android.util.Log.d("SharedIntent", "Received image URI: $imageUri")
                    if (imageUri != null) {
                        database.insertSharedContent(imageUri.toString(), "image")
                        // 获取图片内容并保存到相册
                        try {
                            val inputStream = contentResolver.openInputStream(imageUri)
                            if (inputStream != null) {
                                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                                inputStream.close()
                                
                                // 保存到相册
                                val filename = "CHAOS2045_${System.currentTimeMillis()}.jpg"
                                val contentValues = ContentValues().apply {
                                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Chaos2045")
                                }
                                
                                val uri = contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                                if (uri != null) {
                                    val outputStream = contentResolver.openOutputStream(uri)
                                    if (outputStream != null) {
                                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, outputStream)
                                        outputStream.close()
                                        android.util.Log.d("SharedIntent", "Image saved to gallery: $uri")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SharedIntent", "Error saving image to gallery", e)
                        }
                    }
                }
                "video/*" -> {
                    val videoUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    android.util.Log.d("SharedIntent", "Received video URI: $videoUri")
                    if (videoUri != null) {
                        database.insertSharedContent(videoUri.toString(), "video")
                    }
                }
                "audio/*" -> {
                    val audioUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    android.util.Log.d("SharedIntent", "Received audio URI: $audioUri")
                    if (audioUri != null) {
                        database.insertSharedContent(audioUri.toString(), "audio")
                    }
                }
                "application/*" -> {
                    val fileUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    android.util.Log.d("SharedIntent", "Received file URI: $fileUri")
                    if (fileUri != null) {
                        database.insertSharedContent(fileUri.toString(), "file")
                    }
                }
                else -> {
                    val unknownUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    android.util.Log.d("SharedIntent", "Received unknown type: ${intent.type}, URI: $unknownUri")
                    if (unknownUri != null) {
                        database.insertSharedContent(unknownUri.toString(), "unknown")
                    }
                }
            }
        }
    }
}