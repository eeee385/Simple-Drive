package com.example.myapplication.ui.navigation

sealed class Screen(val route: String) {
    data object Pan : Screen("pan")
    data object Files : Screen("files")
    data object FileList : Screen("file_list/{parentId}") {
        fun createRoute(parentId: String): String = "file_list/$parentId"
    }
    data object Reader : Screen("reader/{fileId}") {
        fun createRoute(fileId: String): String = "reader/$fileId"
    }
}
