package com.example.myapplication

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    private fun handleDeepLink(intent: Intent) {
        val data: Uri? = intent.data
        if (data?.scheme == "simplepan" && data.host == "share") {
            val shareId = data.getQueryParameter("sid")
            if (shareId != null) {
                (application as SimplePanApplication).submitDeepLinkShareId(shareId)
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

    // DeepLink handler: observe pending shareId and navigate
    val app = context.applicationContext as SimplePanApplication
    val pendingShareId by app.pendingShareId.collectAsState()
    LaunchedEffect(pendingShareId) {
        val shareId = pendingShareId ?: return@LaunchedEffect
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val fileId = app.shareRepository.resolveShareLink(shareId)
            if (fileId != null) {
                val file = app.fileRepository.getFileById(fileId)
                if (file != null) {
                    // Navigate to the file's location
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        selectedIndex = 1
                        val targetId = if (file.type == "folder") file.fileId else (file.parentId ?: "root")
                        // First switch to files tab, then navigate to folder
                        navController.navigate(Screen.Files.route) {
                            popUpTo(Screen.Pan.route) { inclusive = true }
                        }
                        navController.navigate(Screen.FileList.createRoute(targetId))
                    }
                }
            }
        }
        app.consumeDeepLinkShareId()
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
