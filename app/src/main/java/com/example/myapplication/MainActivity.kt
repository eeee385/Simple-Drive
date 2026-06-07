package com.example.myapplication

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.navigation.AppNavigation
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.ui.screens.files.FilesScreen
import com.example.myapplication.ui.screens.pan.PanScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

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

        val prefs = getSharedPreferences("deep_link", Context.MODE_PRIVATE)
        val processed = prefs.getStringSet("processed_ids", emptySet()) ?: emptySet()
        if (shareId in processed) return

        prefs.edit().putStringSet("processed_ids", processed + shareId).apply()
        (application as SimplePanApplication).onDeepLinkShareId?.invoke(shareId)
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
)

@Composable
fun MainApp() {
    val navController: NavHostController = rememberNavController()

    val items = listOf(
        BottomNavItem("网盘", Icons.Filled.Cloud, Icons.Outlined.Cloud),
        BottomNavItem("文件", Icons.Filled.Folder, Icons.Outlined.Folder)
    )

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(pageCount = { 2 })

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        com.example.myapplication.util.InitialDataLoader.initialize(context)
    }

    val app = context.applicationContext as SimplePanApplication
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        app.onDeepLinkShareId = { shareId ->
            navController.navigate(Screen.SharePreview.createRoute(shareId))
        }
        onDispose { app.onDeepLinkShareId = null }
    }

    // Sync pager ↔ bottom nav
    LaunchedEffect(pagerState.currentPage) {
        selectedIndex = pagerState.currentPage
    }

    // Whether we're in a sub-screen (reader, share preview, etc.)
    val navBackStackEntry by navController.currentBackStackEntryFlow
        .collectAsState(initial = navController.currentBackStackEntry)
    val currentRoute = navBackStackEntry?.destination?.route
    val isSubScreen = currentRoute != null && currentRoute != Screen.Empty.route
    val hideBottomBar = currentRoute in listOf(
        Screen.Reader.route, Screen.SharePreview.route, Screen.FolderPicker.route,
        Screen.RecentList.route, Screen.FileList.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!hideBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = {
                                selectedIndex = index
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Crossfade(targetState = isSubScreen) { onSubScreen ->
                if (!onSubScreen) {
                    // Main tabs via pager — each page renders the screen directly
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = true
                    ) { page ->
                        when (page) {
                            0 -> PanScreen(navController = navController)
                            1 -> FilesScreen(navController = navController)
                        }
                    }
                }
            }

            // Sub-navigation layer
            AppNavigation(
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
