package com.example.myapplication.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import com.example.myapplication.data.local.db.entity.FileEntity
import com.example.myapplication.util.FileTypeHelper

@Composable
fun FileListItem(
    file: FileEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
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
                if (isSelectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = null)
                } else {
                    Icon(
                        imageVector = FileTypeHelper.getFileIcon(file.type),
                        contentDescription = file.type
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(file.fileId) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onLongPress() }
                    )
                }
        )
    }

    HorizontalDivider()
}
