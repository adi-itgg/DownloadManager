package me.phantomx.downloadmanager.adapter

import me.phantomx.downloadmanager.R
import me.phantomx.downloadmanager.abstracs.adapter.RecyclerViewAdapter
import me.phantomx.downloadmanager.data.FileDownload
import me.phantomx.downloadmanager.database.Repository
import me.phantomx.downloadmanager.extensions.toReadable
import me.phantomx.downloadmanager.sealed.DownloadStatus
import me.phantomx.downloadmanager.services.DownloadService
import kotlin.coroutines.CoroutineContext


class DownloadAdapter(data: MutableList<FileDownload>, coroutineContext: CoroutineContext, downloadService: DownloadService, repository: Repository):
    RecyclerViewAdapter(data, coroutineContext, downloadService, repository) {

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.binding.run {
        position.data().let {
            filename.text = it.filename
            var size =
                "${it.totalDownloaded.toReadable()}/${it.fileSize.toReadable()}"
            if (it.etaDownloadMs > 0 || it.remainingTimeMs > 0) {
                val timeMs = if (it.etaDownloadMs > 0) it.etaDownloadMs else it.remainingTimeMs
                val sec = (timeMs / 1000) % 60
                val minute = (timeMs / (1000*60)) % 60
                val hour = (timeMs / (1000*60*60)) % 24

                var time = ""
                if (hour > 0)
                    time += "${hour}h "
                if (minute > 0)
                    time += "${minute}min "
                if (sec > 0)
                    time += "${sec}sec"

                size += " - $time"
            }
            fileSize.text = size
            val ds =
                "${it.downloadProgress}% ${it.downloadStatus} ${it.downloadSpeed.toReadable()}/s"
            downloadStatus.text = ds
            when (it.downloadStatus) {
                DownloadStatus.Paused -> more.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
                DownloadStatus.Completed -> more.setImageResource(R.drawable.ic_baseline_more_vert_24)
                else -> more.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            }
        }
    }


}