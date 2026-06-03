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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
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

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val paddingPx = with(density) { 16.dp.roundToPx() }
    val textStyle = TextStyle(fontSize = 18.sp, lineHeight = 28.sp, color = Color(0xFF1A1A1A))

    var containerWidth by remember { mutableIntStateOf(0) }
    var containerHeight by remember { mutableIntStateOf(0) }
    var showBars by remember { mutableStateOf(true) }

    // Compute pages synchronously
    val pageWidth = containerWidth - paddingPx * 2
    val pageHeight = containerHeight - paddingPx * 2
    val pages = remember(fullText, pageWidth, pageHeight) {
        if (fullText.isNotEmpty() && pageWidth > 0 && pageHeight > 0) {
            paginate(fullText, measurer, textStyle, pageWidth, pageHeight)
        } else emptyList()
    }

    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    BackHandler { navController.popBackStack() }

    if (isLoading) {
        Scaffold { LoadingOverlay() }
        return
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = showBars, enter = fadeIn(), exit = fadeOut()) {
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
        }
    ) { innerPadding ->
        if (fullText.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                Text("文件内容为空", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
            }
            return@Scaffold
        }

        // Outer Box measures available space, enabling pagination
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .onSizeChanged { containerWidth = it.width; containerHeight = it.height }
        ) {
            if (pages.isEmpty()) {
                Text("分页中...", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures { showBars = !showBars } }
                ) { page ->
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text(text = pages.getOrNull(page)?.text ?: "", style = textStyle)
                    }
                }
            }
        }
    }
}

private fun paginate(
    fullText: String,
    measurer: TextMeasurer,
    style: TextStyle,
    pageWidth: Int,
    pageHeight: Int
): List<PageData> {
    if (fullText.isEmpty()) return emptyList()

    val result = measurer.measure(
        text = AnnotatedString(fullText),
        style = style,
        constraints = Constraints(maxWidth = pageWidth),
        maxLines = Int.MAX_VALUE
    )

    val pages = mutableListOf<PageData>()
    var currentLine = 0
    val totalLines = result.lineCount

    while (currentLine < totalLines) {
        val pageLineStart = currentLine
        var accumulatedHeight = 0f

        while (currentLine < totalLines) {
            val lineHeight = result.getLineBottom(currentLine) - result.getLineTop(currentLine)
            if (accumulatedHeight + lineHeight > pageHeight && accumulatedHeight > 0) break
            accumulatedHeight += lineHeight
            currentLine++
        }

        val startChar = result.getLineStart(pageLineStart)
        val endChar = result.getLineEnd(currentLine - 1, visibleEnd = true)
        pages.add(PageData(text = fullText.substring(startChar, endChar), pageIndex = pages.size))
    }

    return pages
}
