package com.example.myapplication.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.myapplication.data.local.db.entity.FileEntity
import com.example.myapplication.util.FileTypeHelper

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    file: FileEntity,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = if (file.type != "folder") {
            { Text(FileTypeHelper.formatFileSize(file.size)) }
        } else null,
        leadingContent = {
            Icon(
                imageVector = FileTypeHelper.getFileIcon(file.type),
                contentDescription = file.type
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    )
    HorizontalDivider()
}
