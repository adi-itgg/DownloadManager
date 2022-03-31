package me.phantomx.downloadmanager.database

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import me.phantomx.downloadmanager.data.FileDownload
import javax.inject.Inject

class Repository @Inject constructor(private val dao: FileDAO) {

    suspend fun getAll() = withContext(IO) {
        dao.getAll()
    }

    suspend fun get(id: Long) = withContext(IO) {
        dao.get(id)
    }

    suspend fun put(fileDownload: FileDownload) = withContext(IO) {
        dao.put(fileDownload)
    }

    suspend fun update(fileDownload: FileDownload) = withContext(IO) {
        dao.update(fileDownload)
    }

    suspend fun delete(fileDownload: FileDownload) = withContext(IO) {
        dao.delete(fileDownload)
    }

}