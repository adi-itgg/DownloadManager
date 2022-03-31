package me.phantomx.downloadmanager.database.converter

import androidx.room.TypeConverter
import me.phantomx.downloadmanager.sealed.DownloadStatus

class ConvertDownloadStatus {

    @TypeConverter
    fun statusToString(downloadStatus: DownloadStatus) = downloadStatus.toString()

    @TypeConverter
    fun stringToStatus(string: String) = when (string) {
        "Paused" -> DownloadStatus.Paused
        "Queue" -> DownloadStatus.Queue
        "Error" -> DownloadStatus.Error
        "Downloading" -> DownloadStatus.Downloading
        "Completed" -> DownloadStatus.Completed
        else -> DownloadStatus.Queue
    }

}