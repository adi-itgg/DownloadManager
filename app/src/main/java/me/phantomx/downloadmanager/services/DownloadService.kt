package me.phantomx.downloadmanager.services

import androidx.core.net.toUri
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import me.phantomx.downloadmanager.abstracs.services.AppService
import me.phantomx.downloadmanager.data.FileDownload
import me.phantomx.downloadmanager.sealed.DownloadStatus
import okhttp3.Request
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.measureTimeMillis


class DownloadService : AppService() {

    @Suppress("BlockingMethodInNonBlockingContext")
    fun runJob(fileDownload: FileDownload) = fileDownload.run {
        val fileLocation = File(saveLocation)
        val isResume = downloadStatus == DownloadStatus.Paused && fileLocation.exists()
        downloadStatus = DownloadStatus.Downloading
        liveData.postValue(this)
        downloadJobs.add(this)
        downloadJob = launch(downloadDispatcher) {
            try {
                okclient.apply {
                    val req = Request.Builder().url(url).get()
                    if (isResume)
                        req.addHeader("Range", "bytes=${fileLocation.length()}-")
                    newCall(req.build()).execute().apply {
                        body?.apply {
                            byteStream().use {
                                contentResolver.openOutputStream(
                                    fileLocation.toUri(),
                                    if (isResume) "wa" else "w"
                                )?.use { outputStream ->

                                    launch(Default) {
                                        launch(Default) {
                                            while (true) {
                                                delay(1000)
                                                if (downloadSpeed > 0) {
                                                    val size = fileSize - totalDownloaded
                                                    val timeInSec = size / downloadSpeed
                                                    remainingTimeMs =
                                                        (timeInSec * 1000L) + 1000L
                                                }
                                                when (downloadStatus) {
                                                    DownloadStatus.Downloading -> if (downloadProgress == 100) break
                                                    else -> break
                                                }
                                            }
                                        }
                                        var lastDownloadedSize = 0L
                                        val historySpeed: MutableList<Long> = ArrayList()
                                        while (true) {
                                            when (downloadStatus) {
                                                DownloadStatus.Downloading -> if (downloadProgress == 100) break
                                                else -> break
                                            }
                                            if (lastDownloadedSize > 0L) {
                                                val liveDownloadSpeed =
                                                    totalDownloaded - lastDownloadedSize
                                                lastDownloadedSize = totalDownloaded
                                                historySpeed.add(liveDownloadSpeed)
                                                if (historySpeed.size > 5) historySpeed.removeAt(0)

                                                var totalSpeed = 0L
                                                for (speed in historySpeed)
                                                    totalSpeed += speed
                                                downloadSpeed = totalSpeed / historySpeed.size
                                            } else
                                                lastDownloadedSize = totalDownloaded

                                            liveData.postValue(this@run)
                                            delay(1000)
                                        }
                                    }

                                    if (fileSize <= 0L)
                                        fileSize = contentLength()

                                    etaDownloadMs += measureTimeMillis {
                                        val chunkSize = ByteArray(4096)
                                        var byteRead: Int
                                        while (it.read(chunkSize, 0, chunkSize.size)
                                                .also { byteRead = it } != -1
                                        ) {
                                            totalDownloaded += byteRead
                                            outputStream.write(chunkSize, 0, byteRead)
                                            var progress =
                                                (totalDownloaded.toDouble() / fileSize.toDouble() * 100.0).toInt()
                                            if (progress < 0) progress = 0
                                            downloadProgress = progress
                                            when (downloadStatus) {
                                                DownloadStatus.Paused -> break
                                                else -> yield()
                                            }
                                        }
                                    }

                                    outputStream.flush()
                                }
                            }
                        }
                    }
                }
                if (downloadStatus != DownloadStatus.Paused) {
                    dateCompleted = Calendar.getInstance().timeInMillis
                    downloadStatus = DownloadStatus.Completed
                    downloadProgress = 100
                }
            } catch (e: CancellationException) {
                e.message?.let {
                    when (it) {
                        "Pause" -> downloadStatus = DownloadStatus.Paused
                    }
                }
            } catch (e: Exception) {
                downloadStatus = DownloadStatus.Paused
                if (e is CancellationException) throw e
                errorMsg = e.message ?: "Unknown"
                downloadStatus = DownloadStatus.Error
                e.printStackTrace()
            }
            downloadJob = null
            downloadJobs.remove(this@run)
            liveData.postValue(this@run)
            if (downloadJobs.isEmpty())
                notifRunning = false
            repository.update(this@run)
        }
        if (!notifRunning) {
            notifRunning = true
            CoroutineScope(Default).launch {
                var count = 0
                while (notifRunning) {
                    if (count >= 10) {
                        notificationStatus()
                        count = 0
                    }
                    for (job in downloadJobs)
                        repository.update(job)
                    count++
                    delay(100)
                }
                notificationStatus()
            }
        }
    }

}