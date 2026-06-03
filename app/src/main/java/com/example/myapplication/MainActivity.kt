package com.example.myapplication

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
        // 延迟以保证回调成功注册
        window.decorView.post {
            checkClipboardForDeepLink()
        }
    }

    private fun checkClipboardForDeepLink() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clipText = clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.text?.toString() ?: return

        val prefix = "simplepan://share?sid="
        if (!clipText.startsWith(prefix)) return
        val shareId = clipText.removePrefix(prefix)
        if (shareId.isBlank()) return

        // Persist processed shareIds so same link doesn't trigger again across restarts
        val prefs = getSharedPreferences("deep_link", Context.MODE_PRIVATE)
        val processed = prefs.getStringSet("processed_ids", emptySet()) ?: emptySet()
        if (shareId in processed) return

        prefs.edit().putStringSet("processed_ids", processed + shareId).apply()
        (application as SimplePanApplication).onDeepLinkShareId?.invoke(shareId)
        // 获取当前进程的Application对象 as 强制类型转换为 SimplePanApplication类型
        // 这里程序invoke拿到了shareid，就会调用之前注册的lambda
    }

    private fun handleDeepLink(intent: Intent) {
        val data: Uri = intent.data ?: return
        if (data.scheme == "simplepan" && data.host == "share") {
            val shareId = data.getQueryParameter("sid") ?: return
            (application as SimplePanApplication).onDeepLinkShareId?.invoke(shareId)
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

    // 此处就是 注册回调 告诉simplepan你拿到了shareid后就调用这个lambda，去打开预览页
    // DeepLink handler: register callback that Activity invokes directly
    val app = context.applicationContext as SimplePanApplication
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        app.onDeepLinkShareId = { shareId ->
            navController.navigate(Screen.SharePreview.createRoute(shareId))
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
