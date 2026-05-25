package com.example.myapplication.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.myapplication.data.local.db.entity.ShareLinkEntity

@Dao
interface ShareLinkDao {

    @Insert
    suspend fun insert(link: ShareLinkEntity)

    @Query("SELECT * FROM share_links WHERE shareId = :shareId")
    suspend fun getShareLink(shareId: String): ShareLinkEntity?
}
