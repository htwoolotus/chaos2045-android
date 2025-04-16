package com.example.chaos2045

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.chaos2045.database.SharedContentDatabase
import com.example.chaos2045.database.SharedContentEntity
import com.example.chaos2045.ui.BakingScreen
import com.example.chaos2045.ui.ContentDetailScreen
import com.example.chaos2045.ui.SetupScreen
import com.example.chaos2045.ui.SharedContentScreen
import com.example.chaos2045.ui.theme.Chaos2045Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

sealed class Screen(val route: String, val icon: @Composable () -> Unit, val label: String) {
    object SharedContent : Screen("shared_content", { Icon(Icons.Default.List, "Shared Content") }, "Shared Content")
    object Setup : Screen("setup", { Icon(Icons.Default.Settings, "Settings") }, "Settings")
}

class MainActivity : ComponentActivity() {
    private lateinit var database: SharedContentDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = SharedContentDatabase.getDatabase(this)
        setTitle(R.string.app_name)

        setContent {
            Chaos2045Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    val items = listOf(Screen.SharedContent, Screen.Setup)
                    
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                val navBackStackEntry by navController.currentBackStackEntryAsState()
                                val currentDestination = navBackStackEntry?.destination
                                
                                items.forEach { screen ->
                                    NavigationBarItem(
                                        icon = screen.icon,
                                        label = { Text(screen.label) },
                                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { paddingValues ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.SharedContent.route,
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            composable(Screen.SharedContent.route) {
                                SharedContentScreen(
                                    database = database,
                                    onNavigateToDetail = { contentId ->
                                        navController.navigate("content_detail/$contentId")
                                    }
                                )
                            }
                            composable(Screen.Setup.route) {
                                SetupScreen(database = database)
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
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun saveImageToGallery(imageUri: Uri?) {
        // 获取图片内容并保存到相册
        if (imageUri != null) {
            database.sharedContentDao().insertContent(SharedContentEntity(content = imageUri.toString(), type = "image"))
            // 获取图片内容并保存到相册
            try {
                val inputStream = contentResolver.openInputStream(imageUri)
                if (inputStream != null) {
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    
                    // 保存到相册
                    val filename = "CHAOS2045_${System.currentTimeMillis()}.jpg"
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Chaos2045")
                    }
                    
                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        val outputStream = contentResolver.openOutputStream(uri)
                        if (outputStream != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                            outputStream.close()
                            Log.d("SharedIntent", "Image saved to gallery: $uri")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SharedIntent", "Error saving image to gallery", e)
            }
        }
    }

    // 实现远程图片内容接收接口 (I0004)
    private fun sendImageToRemoteApi(imageUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "http://easyde.vip/post-image-content"
                
                // 读取图片数据
                val inputStream = contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                
                // 准备图片数据
                val client = OkHttpClient()
                
                // 创建MultipartBody
                val requestBody = okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("source", "CHAOS2045")
                    .addFormDataPart(
                        "image", 
                        "shared_image.jpg",
                        RequestBody.create(
                            "image/jpeg".toMediaTypeOrNull(),
                            bitmap.toString().toByteArray()
                        )
                    )
                    .build()
                
                // 创建请求
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()
                
                // 执行请求
                val response = client.newCall(request).execute()
                
                // 处理响应
                if (response.isSuccessful) {
                    Log.d("SharedIntent", "Image sent to remote API successfully")
                } else {
                    Log.e("SharedIntent", "Failed to send image to remote API: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("SharedIntent", "Error sending image to remote API", e)
            }
        }
    }

    // 实现远程文本内容接收接口 (I0005)
    private fun sendTextToRemoteApi(text: String) {
        // 创建一个协程作用域来执行网络请求
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 准备请求URL
                val url = "http://easyde.vip/post-text-content"
                
                // 创建OkHttp客户端
                val client = okhttp3.OkHttpClient()
                
                // 准备JSON数据
                val json = "application/json; charset=utf-8".toMediaType()
                val requestBody = okhttp3.RequestBody.create(
                    json,
                    """{"content": "$text", "source": "CHAOS2045"}"""
                )
                
                // 创建请求
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()
                
                // 执行请求
                val response = client.newCall(request).execute()
                
                // 处理响应
                if (response.isSuccessful) {
                    Log.d("SharedIntent", "Text sent to remote API successfully")
                } else {
                    // 在主线程中显示Toast提示
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to send text: ${response.code}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    Log.e("SharedIntent", "Failed to send text to remote API: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("SharedIntent", "Error sending text to remote API", e)
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            // 后台打印 intent.type 内容
            Log.d("SharedIntent", "Received intent type: ${intent?.type}")
            when (intent.type) {
                "text/plain" -> {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (sharedText != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            database.sharedContentDao().insertContent(SharedContentEntity(content = sharedText, type = "text"))
                            // 将文本内容发送到远程接口
                            sendTextToRemoteApi(sharedText)
                        }
                    }
                }
                "image/jpeg" -> {
                    val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    Log.d("SharedIntent", "Received image URI: $imageUri")
                    // 获取图片内容并保存到相册
                    if (imageUri != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            database.sharedContentDao().insertContent(SharedContentEntity(content = imageUri.toString(), type = "image"))
                            saveImageToGallery(imageUri)
                            // 将图片内容发送到远程接口
                            sendImageToRemoteApi(imageUri)
                        }
                    }
                }
                "video/*" -> {
                    val videoUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    Log.d("SharedIntent", "Received video URI: $videoUri")
                    if (videoUri != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            database.sharedContentDao().insertContent(SharedContentEntity(content = videoUri.toString(), type = "video"))
                        }
                    }
                }
                "audio/*" -> {
                    val audioUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    Log.d("SharedIntent", "Received audio URI: $audioUri")
                    if (audioUri != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            database.sharedContentDao().insertContent(SharedContentEntity(content = audioUri.toString(), type = "audio"))
                        }
                    }
                }
                "application/*" -> {
                    val fileUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    Log.d("SharedIntent", "Received file URI: $fileUri")
                    if (fileUri != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            database.sharedContentDao().insertContent(SharedContentEntity(content = fileUri.toString(), type = "file"))
                        }
                    }
                }
                else -> {
                    val unknownUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    Log.d("SharedIntent", "Received unknown type: ${intent.type}, URI: $unknownUri")
                    if (unknownUri != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            database.sharedContentDao().insertContent(SharedContentEntity(content = unknownUri.toString(), type = "unknown"))
                        }
                    }
                }
            }
        }
    }
}