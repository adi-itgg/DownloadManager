package me.phantomx.downloadmanager.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import me.phantomx.downloadmanager.data.FileDownload
import me.phantomx.downloadmanager.database.converter.ConvertDownloadStatus

@Database(entities = [FileDownload::class], version = 1, exportSchema = false)
@TypeConverters(ConvertDownloadStatus::class)
abstract class Database: RoomDatabase() {
    abstract fun dao(): FileDAO
}