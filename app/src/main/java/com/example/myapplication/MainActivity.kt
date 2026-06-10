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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun MainApp() {
    val navController: NavHostController = rememberNavController()

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(pageCount = { 2 })

    // Files tab selection state
    var isFilesSelecting by remember { mutableStateOf(false) }
    var filesSelectCount by remember { mutableIntStateOf(0) }
    var isFilesAllSelected by remember { mutableStateOf(false) }
    var filesDismissAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var filesToggleAllAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Files tab folder state
    var isInSubFolder by remember { mutableStateOf(false) }
    var subFolderName by remember { mutableStateOf("") }
    var filesNavigateBack by remember { mutableStateOf<(() -> Unit)?>(null) }

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

    LaunchedEffect(pagerState.currentPage) {
        selectedIndex = pagerState.currentPage
    }

    val navBackStackEntry by navController.currentBackStackEntryFlow
        .collectAsState(initial = navController.currentBackStackEntry)
    val currentRoute = navBackStackEntry?.destination?.route
    val isSubScreen = currentRoute != null && currentRoute != Screen.Empty.route
    val hideTabBar = currentRoute in listOf(
        Screen.Reader.route,
        Screen.FileList.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Header area — shown when not on full-screen pages
            if (!hideTabBar) {
                if (isSubScreen && currentRoute?.startsWith("recent_list") == true) {
                    val listType = navBackStackEntry?.arguments?.getString("listType") ?: "browse"
                    val title = if (listType == "browse") "最近浏览" else "最近转存"
                    SubPageHeader(title = title, onBack = { navController.popBackStack() }, fontSize = 22.sp)
                } else if (isSubScreen && currentRoute?.startsWith("share_preview") == true) {
                    SubPageHeader(title = "分享文件", onBack = { navController.popBackStack() })
                } else if (isFilesSelecting && selectedIndex == 1) {
                    SelectionHeader(
                        selectCount = filesSelectCount,
                        allSelected = isFilesAllSelected,
                        onDismiss = { filesDismissAction?.invoke() },
                        onToggleAll = { filesToggleAllAction?.invoke() }
                    )
                } else if (isInSubFolder && selectedIndex == 1) {
                    SubFolderHeader(
                        folderName = subFolderName,
                        onBack = { filesNavigateBack?.invoke() }
                    )
                } else if (!isSubScreen) {
                    TopTabBar(
                        selectedIndex = selectedIndex,
                        onTabClick = { index ->
                            selectedIndex = index
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // NavHost behind — always composed so navigation works
                AppNavigation(
                    navController = navController,
                    modifier = Modifier.fillMaxSize()
                )

                // Pager on top, hidden when on sub-screen
                if (!isSubScreen) {
                    Column(modifier = Modifier.fillMaxSize().zIndex(1f)) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = !isFilesSelecting && !isInSubFolder && !isSubScreen
                        ) { page ->
                            when (page) {
                                0 -> PanScreen(navController = navController)
                                1 -> FilesScreen(
                                    navController = navController,
                                    onSelectionChanged = { selecting, count, allSelected, dismiss, toggleAll ->
                                        isFilesSelecting = selecting
                                        filesSelectCount = count
                                        isFilesAllSelected = allSelected
                                        filesDismissAction = dismiss
                                        filesToggleAllAction = toggleAll
                                    },
                                    onFolderChanged = { inSubFolder, name, onBack ->
                                        isInSubFolder = inSubFolder
                                        subFolderName = name
                                        filesNavigateBack = onBack
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionHeader(
    selectCount: Int,
    allSelected: Boolean,
    onDismiss: () -> Unit,
    onToggleAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(88.dp), contentAlignment = Alignment.CenterStart) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "取消选择"
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "管理文件",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "已选择 $selectCount 项",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(modifier = Modifier.width(88.dp), contentAlignment = Alignment.Center) {
            TextButton(onClick = onToggleAll) {
                Text(if (allSelected) "取消全选" else "全选")
            }
        }
    }
}

@Composable
private fun SubPageHeader(
    title: String,
    onBack: () -> Unit,
    fontSize: TextUnit = MaterialTheme.typography.titleMedium.fontSize
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(48.dp)) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
        }
        Text(
            title,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun SubFolderHeader(
    folderName: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(48.dp)) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
        }
        Text(
            folderName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Box(modifier = Modifier.width(48.dp))
    }
}

@Composable
private fun TopTabBar(
    selectedIndex: Int,
    onTabClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TabText("网盘", selected = selectedIndex == 0) { onTabClick(0) }
        Spacer(Modifier.width(24.dp))
        TabText("文件", selected = selectedIndex == 1) { onTabClick(1) }
    }
}

@Composable
private fun TabText(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val fontSize by animateFloatAsState(
        targetValue = if (selected) 22f else 16f,
        label = "fontSize"
    )
    val color by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onBackground
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabColor"
    )

    Text(
        text = text,
        fontSize = fontSize.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}
