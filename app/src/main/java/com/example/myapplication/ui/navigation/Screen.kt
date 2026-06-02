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
    data object RecentList : Screen("recent_list/{listType}") {
        fun createRoute(listType: String): String = "recent_list/$listType"
    }
    data object FolderPicker : Screen("folder_picker/{parentId}") {
        fun createRoute(parentId: String = "root"): String = "folder_picker/$parentId"
    }
    data object SharePreview : Screen("share_preview/{shareId}") {
        fun createRoute(shareId: String): String = "share_preview/$shareId"
    }
}
