package me.phantomx.downloadmanager.database

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import me.phantomx.downloadmanager.data.FileDownload

@Dao
interface FileDAO {

    @Query("SELECT * FROM filedownload")
    suspend fun getAll(): List<FileDownload>

    @Query("SELECT * FROM filedownload WHERE id = :id")
    suspend fun get(id: Long): FileDownload?

    @Insert(onConflict = REPLACE)
    suspend fun put(fileDownload: FileDownload)

    @Update
    suspend fun update(fileDownload: FileDownload)

    @Delete
    suspend fun delete(fileDownload: FileDownload)

}