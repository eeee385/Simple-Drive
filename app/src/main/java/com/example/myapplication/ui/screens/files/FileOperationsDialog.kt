package com.example.myapplication.ui.screens.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.myapplication.data.local.db.entity.FileEntity

@Composable
fun FileContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("重命名") }, onClick = {
            onDismiss()
            onRename()
        })
        DropdownMenuItem(text = { Text("移动") }, onClick = {
            onDismiss()
            onMove()
        })
        DropdownMenuItem(text = { Text("删除") }, onClick = {
            onDismiss()
            onDelete()
        })
        DropdownMenuItem(text = { Text("分享") }, onClick = {
            onDismiss()
            onShare()
        })
    }
}

@Composable
fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名") },
        text = {
            TextField(value = newName, onValueChange = { newName = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(newName) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun MoveFileDialog(
    folders: List<FileEntity>,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("移动到") },
        text = {
            if (folders.isEmpty()) {
                Text("没有可用的文件夹")
            } else {
                androidx.compose.foundation.layout.Column {
                    folders.forEach { folder ->
                        Text(
                            text = folder.name,
                            modifier = Modifier
                                .clickable { onConfirm(folder.fileId) }
                                .padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(null) }) { Text("移动到根目录") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    fileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = { Text("确定要删除「$fileName」吗？此操作不可撤销。") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("删除") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
