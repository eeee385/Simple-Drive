package com.example.myapplication.domain.service

import com.example.myapplication.data.local.db.entity.FileEntity
import com.example.myapplication.domain.model.FileCategory
import com.example.myapplication.domain.model.FilterType

object FileService {

    private val typeOrder = mapOf(
        FileCategory.FOLDER to 0,
        FileCategory.TXT to 1,
        FileCategory.IMAGE to 2,
        FileCategory.VIDEO to 3,
        FileCategory.AUDIO to 4
    )

    val sortComparator = Comparator<FileEntity> { a, b ->
        val typeA = typeOrder[a.type] ?: 99
        val typeB = typeOrder[b.type] ?: 99
        if (typeA != typeB) typeA - typeB
        else a.name.lowercase().compareTo(b.name.lowercase())
    }

    fun sort(list: List<FileEntity>): List<FileEntity> = list.sortedWith(sortComparator)

    fun filter(list: List<FileEntity>, filterType: FilterType): List<FileEntity> = when (filterType) {
        FilterType.ALL -> list
        FilterType.IMAGE -> list.filter { it.type == FileCategory.IMAGE }
        FilterType.VIDEO -> list.filter { it.type == FileCategory.VIDEO }
        FilterType.DOC -> list.filter { FileCategory.isDocument(it.type) }
    }
}
