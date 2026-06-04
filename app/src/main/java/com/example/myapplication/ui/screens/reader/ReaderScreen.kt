package com.example.myapplication.ui.screens.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.SimplePanApplication
import com.example.myapplication.ui.components.LoadingOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PageData(val text: String, val pageIndex: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as SimplePanApplication
    val fileId = navController.currentBackStackEntry?.arguments?.getString("fileId") ?: ""

    val viewModel: ReaderViewModel = viewModel(factory = ReaderViewModel.Factory(app.fileRepository))
    val fullText by viewModel.fullText.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(fileId) {
        withContext(Dispatchers.IO) { viewModel.initialize(context, fileId) }
    }

    val density = LocalDensity.current
    val paddingPx = with(density) { 16.dp.roundToPx() }
    val textStyle = TextStyle(fontSize = 18.sp, lineHeight = 28.sp, color = Color(0xFF1A1A1A))

    // 通过by remember随时动态更改屏幕尺寸
    var containerWidth by remember { mutableIntStateOf(0) }
    var containerHeight by remember { mutableIntStateOf(0) }
    // Auto-reset when fullText changes (new file)
    var pages by remember(fullText) { mutableStateOf<List<PageData>>(emptyList()) }

    // Paginate on background thread
    val pageWidth = containerWidth - paddingPx * 2
    val pageHeight = containerHeight - paddingPx * 2
    val densityFloat = density.density
    LaunchedEffect(fullText, pageWidth, pageHeight) {
        if (fullText.isNotEmpty() && pageWidth > 0 && pageHeight > 0) {
            pages = withContext(Dispatchers.Default) {
                paginateWithStaticLayout(fullText, pageWidth, pageHeight, densityFloat)
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    BackHandler { navController.popBackStack() }

    // Show loading while text or pages aren't ready
    val showLoading = isLoading || (fullText.isNotEmpty() && pages.isEmpty())

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(fileName.ifEmpty { "阅读" }, maxLines = 1)
                            if (pages.isNotEmpty()) {
                                Text(
                                    "${pagerState.currentPage + 1} / ${pages.size}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
                if (pages.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { (pagerState.currentPage + 1).toFloat() / pages.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { innerPadding ->
        // Outer Box measures available space, enabling pagination (always renders)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .onSizeChanged { containerWidth = it.width; containerHeight = it.height } //动态适配当前的屏幕尺寸
        ) {
            if (showLoading) {
                LoadingOverlay()
            } else if (fullText.isEmpty()) {
                Text("文件内容为空", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
            } else {
                HorizontalPager(
                    state = pagerState,
                ) { page ->
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text(text = pages.getOrNull(page)?.text ?: "", style = textStyle)
                    }
                }
            }
        }
    }
}

private fun paginateWithStaticLayout(
    fullText: String,
    pageWidthPx: Int,
    pageHeightPx: Int,
    density: Float
): List<PageData> {
    if (fullText.isEmpty() || pageWidthPx <= 0 || pageHeightPx <= 0) return emptyList()

    val paint = android.text.TextPaint().apply {
        textSize = 18f * density
        isAntiAlias = true
    }

    val layout = android.text.StaticLayout.Builder
        .obtain(fullText, 0, fullText.length, paint, pageWidthPx)
        .build()

    val pages = mutableListOf<PageData>()
    var currentLine = 0
    val totalLines = layout.lineCount

    // Use Compose's actual line height for page calculation, not StaticLayout's
    val composeLineHeight = 28f * density

    while (currentLine < totalLines) {
        val pageLineStart = currentLine

        while (currentLine < totalLines) {
            val linesInPage = currentLine - pageLineStart + 1
            if (linesInPage * composeLineHeight > pageHeightPx && currentLine > pageLineStart) break
            currentLine++
        }

        val startChar = layout.getLineStart(pageLineStart)
        val endChar = layout.getLineEnd(currentLine - 1)
        pages.add(PageData(text = fullText.substring(startChar, endChar), pageIndex = pages.size))
    }

    return pages
}
