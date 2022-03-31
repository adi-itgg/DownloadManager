package me.phantomx.downloadmanager.listeners

import me.phantomx.downloadmanager.data.FileDownload

interface AddJobListener {

    fun onNewJob(fileDownload: FileDownload)

}