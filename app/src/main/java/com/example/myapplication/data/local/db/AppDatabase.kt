package com.example.myapplication.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.local.db.dao.FileDao
import com.example.myapplication.data.local.db.dao.RecentBrowseDao
import com.example.myapplication.data.local.db.dao.RecentTransferDao
import com.example.myapplication.data.local.db.dao.ShareLinkDao
import com.example.myapplication.data.local.db.entity.FileEntity
import com.example.myapplication.data.local.db.entity.RecentBrowseEntity
import com.example.myapplication.data.local.db.entity.RecentTransferEntity
import com.example.myapplication.data.local.db.entity.ShareLinkEntity

@Database(
    entities = [
        FileEntity::class,
        RecentBrowseEntity::class,
        RecentTransferEntity::class,
        ShareLinkEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun recentBrowseDao(): RecentBrowseDao
    abstract fun recentTransferDao(): RecentTransferDao
    abstract fun shareLinkDao(): ShareLinkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "simplepan.db")
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
