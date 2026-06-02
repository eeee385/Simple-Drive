package com.example.myapplication

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.navigation.AppNavigation
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLink(intent)
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    override fun onResume() {
        super.onResume()
        // Delay to next frame so Compose DisposableEffect has registered the callback
        window.decorView.post {
            checkClipboardForDeepLink()
        }
    }

    private var lastProcessedShareId: String? = null

    private fun checkClipboardForDeepLink() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = clipboard.primaryClip
        val clipText = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()

        // Also check SharedPreferences as fallback
        val prefs = getSharedPreferences("deep_link", Context.MODE_PRIVATE)
        val savedShareId = prefs.getString("pending_share_id", null)
        val shareId = clipText?.removePrefix("simplepan://share?sid=").takeIf { !it.isNullOrBlank() }
            ?: savedShareId

        if (shareId == null || shareId == lastProcessedShareId) return

        Toast.makeText(this, "检测到待跳转: $shareId", Toast.LENGTH_LONG).show()
        lastProcessedShareId = shareId
        prefs.edit().remove("pending_share_id").apply()
        handleDeepLinkShareId(shareId)
    }

    private fun handleDeepLinkShareId(shareId: String) {
        val callback = (application as SimplePanApplication).onDeepLinkShareId
        Toast.makeText(this, "回调: ${if (callback != null) "已设置" else "未设置!!"}", Toast.LENGTH_LONG).show()
        callback?.invoke(shareId)
    }

    private fun handleDeepLink(intent: Intent) {
        val data: Uri = intent.data ?: return
        if (data.scheme == "simplepan" && data.host == "share") {
            val shareId = data.getQueryParameter("sid")
            if (shareId != null) {
                Toast.makeText(this, "收到DeepLink: $shareId", Toast.LENGTH_LONG).show()
                handleDeepLinkShareId(shareId)
            }
        }
    }
}

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

@Composable
fun MainApp() {
    val navController: NavHostController = rememberNavController()

    val items = listOf(
        BottomNavItem("网盘", Icons.Filled.Cloud, Icons.Outlined.Cloud, Screen.Pan.route),
        BottomNavItem("文件", Icons.Filled.Folder, Icons.Outlined.Folder, Screen.Files.route)
    )

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    // Initialize mock data on first launch
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        com.example.myapplication.util.InitialDataLoader.initialize(context)
    }

    // DeepLink handler: register callback that Activity invokes directly
    val app = context.applicationContext as SimplePanApplication
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        app.onDeepLinkShareId = { shareId ->
            android.widget.Toast.makeText(context, "处理DeepLink: $shareId", android.widget.Toast.LENGTH_SHORT).show()
            scope.launch {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val fileId = app.shareRepository.resolveShareLink(shareId)
                    android.util.Log.d("DeepLink", "resolveShareLink: shareId=$shareId fileId=$fileId")
                    if (fileId == null) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "fileId为空，shareId无效", android.widget.Toast.LENGTH_LONG).show()
                        }
                        return@withContext
                    }
                    val file = app.fileRepository.getFileById(fileId)
                    android.util.Log.d("DeepLink", "getFileById: file=${file?.name} type=${file?.type}")
                    if (file == null) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "file为空，fileId=$fileId", android.widget.Toast.LENGTH_LONG).show()
                        }
                        return@withContext
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        selectedIndex = 1
                        val targetId = if (file.type == "folder") file.fileId else (file.parentId ?: "root")
                        android.widget.Toast.makeText(context, "导航到: $targetId", android.widget.Toast.LENGTH_LONG).show()
                        navController.navigate(Screen.Files.route) {
                            popUpTo(Screen.Pan.route) { inclusive = true }
                        }
                        navController.navigate(Screen.FileList.createRoute(targetId))
                    }
                }
            }
        }
        onDispose { app.onDeepLinkShareId = null }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = {
                            selectedIndex = index
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        AppNavigation(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
