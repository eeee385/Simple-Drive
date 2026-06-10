package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.myapplication.ui.screens.files.FilesScreen
import com.example.myapplication.ui.screens.pan.RecentListScreen
import com.example.myapplication.ui.screens.pan.SharePreviewScreen
import com.example.myapplication.ui.screens.reader.ReaderScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Empty.route,
        modifier = modifier,
        enterTransition = { fadeIn(tween(0)) },
        exitTransition = { fadeOut(tween(0)) },
        popEnterTransition = { fadeIn(tween(0)) },
        popExitTransition = { fadeOut(tween(0)) }
    ) {
        composable(Screen.Empty.route) {
            // Placeholder — main tabs are rendered by the pager
        }
        composable(
            route = Screen.FileList.route,
            arguments = listOf(navArgument("parentId") { type = NavType.StringType })
        ) {
            FilesScreen(navController = navController)
        }
        composable(
            route = Screen.Reader.route,
            arguments = listOf(navArgument("fileId") { type = NavType.StringType })
        ) {
            ReaderScreen(navController = navController)
        }
        composable(
            route = Screen.RecentList.route,
            arguments = listOf(navArgument("listType") { type = NavType.StringType })
        ) { backStackEntry ->
            val listType = backStackEntry.arguments?.getString("listType") ?: "browse"
            RecentListScreen(listType = listType, navController = navController)
        }
        composable(
            route = Screen.SharePreview.route,
            arguments = listOf(navArgument("shareId") { type = NavType.StringType })
        ) { backStackEntry ->
            val shareId = backStackEntry.arguments?.getString("shareId") ?: return@composable
            SharePreviewScreen(
                shareId = shareId,
                onDismiss = { navController.popBackStack() },
                onTransfer = {
                    android.widget.Toast.makeText(
                        navController.context, "转存成功",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    navController.popBackStack()
                }
            )
        }
    }
}
