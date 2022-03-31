package me.phantomx.downloadmanager.data

import androidx.room.*
import kotlinx.coroutines.Job
import me.phantomx.downloadmanager.database.converter.ConvertDownloadStatus
import me.phantomx.downloadmanager.sealed.DownloadStatus
import java.io.Serializable

@Entity(tableName = "filedownload")
data class FileDownload(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val filename: String,
    @ColumnInfo(name = "download_status")
    @TypeConverters(ConvertDownloadStatus::class)
    var downloadStatus: DownloadStatus,
    @ColumnInfo(name = "file_size")
    var fileSize: Long,
    var url: String,
    @ColumnInfo(name = "save_location")
    var saveLocation: String,
    @ColumnInfo(name = "date_added")
    var dateAdded: Long = 0L,
    @ColumnInfo(name = "date_completed")
    var dateCompleted: Long = 0L,
    @ColumnInfo(name = "total_downloaded")
    var totalDownloaded: Long = 0L,
    @ColumnInfo(name = "eta_download_ms")
    var etaDownloadMs: Long = 0L,
    @ColumnInfo(name = "remaining_time_ms")
    var remainingTimeMs: Long = 0L,
    @ColumnInfo(name = "download_speed")
    var downloadSpeed: Long = 0L,
    @ColumnInfo(name = "download_progress")
    var downloadProgress: Int = 0,
    @ColumnInfo(name = "error_msg")
    var errorMsg: String = ""
): Serializable {
    @Ignore
    var downloadJob: Job? = null

    override fun equals(other: Any?): Boolean {
        return (other as FileDownload).let {
            id == it.id && saveLocation == it.saveLocation
        }
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + saveLocation.hashCode()
        return result
    }
}