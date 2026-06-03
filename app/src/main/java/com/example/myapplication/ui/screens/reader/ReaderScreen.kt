package com.example.myapplication.ui.screens.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

    val viewModel: ReaderViewModel = viewModel(
        factory = ReaderViewModel.Factory(app.fileRepository)
    )

    val fullText by viewModel.fullText.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(fileId) {
        withContext(Dispatchers.IO) { viewModel.initialize(context, fileId) }
    }

    if (isLoading) {
        Scaffold { LoadingOverlay() }
        return
    }

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val paddingPx = with(density) { 16.dp.roundToPx() }
    val textStyle = TextStyle(fontSize = 18.sp, lineHeight = 28.sp, color = Color(0xFF1A1A1A))

    // Compute pages based on available screen size
    var pages by remember { mutableStateOf<List<PageData>>(emptyList()) }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
    var showBars by remember { mutableStateOf(true) }

    BackHandler { navController.popBackStack() }

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = showBars, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(fileName.ifEmpty { "阅读" }, maxLines = 1)
                                if (pages.isNotEmpty()) {
                                    Text("${pagerState.currentPage + 1} / ${pages.size}", style = MaterialTheme.typography.bodySmall)
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

        // Measure available space and paginate
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val availableWidthPx = with(density) { maxWidth.roundToPx() } - paddingPx * 2
            val availableHeightPx = with(density) { maxHeight.roundToPx() } - paddingPx * 2

            // Paginate when dimensions are known
            LaunchedEffect(fullText, maxWidth, maxHeight) {
                if (availableWidthPx > 0 && availableHeightPx > 0) {
                    pages = paginate(
                        fullText = fullText,
                        measurer = measurer,
                        style = textStyle,
                        pageWidth = availableWidthPx,
                        pageHeight = availableHeightPx
                    )
                }
            }

            if (pages.isEmpty()) return@BoxWithConstraints

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { showBars = !showBars } }
            ) { page ->
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = pages.getOrNull(page)?.text ?: "",
                        style = textStyle
                    )
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
    val pages = mutableListOf<PageData>()
    var offset = 0

    while (offset < fullText.length) {
        val result = measurer.measure(
            text = AnnotatedString(fullText.substring(offset)),
            style = style,
            constraints = Constraints(maxWidth = pageWidth),
            maxLines = Int.MAX_VALUE
        )

        var lastLine = 0
        var accumulatedHeight = 0f
        for (i in 0 until result.lineCount) {
            val lineH = result.getLineBottom(i) - result.getLineTop(i)
            if (accumulatedHeight + lineH > pageHeight) break
            accumulatedHeight += lineH
            lastLine = i + 1
        }
        if (lastLine == 0) lastLine = 1

        val endOffset = result.getLineEnd(lastLine - 1, visibleEnd = true)
        pages.add(PageData(text = fullText.substring(offset, offset + endOffset), pageIndex = pages.size))
        offset += endOffset
    }

    return pages
}
