package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.myapplication.ui.screens.files.FilesScreen
import com.example.myapplication.ui.screens.files.FolderPickerScreen
import com.example.myapplication.ui.screens.pan.PanScreen
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
        startDestination = Screen.Pan.route,
        modifier = modifier
    ) {
        composable(Screen.Pan.route) {
            PanScreen(navController = navController)
        }
        composable(Screen.Files.route) {
            FilesScreen(navController = navController)
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
        composable(
            route = Screen.FolderPicker.route,
            arguments = listOf(navArgument("parentId") { type = NavType.StringType; defaultValue = "root" })
        ) {
            val moveIds = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<List<String>>("move_ids") ?: emptyList()
            FolderPickerScreen(
                initialParentId = "root",
                excludedFolderIds = moveIds.toSet(),
                onFolderSelected = { folderId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "picker_result",
                        folderId ?: "root"
                    )
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}
