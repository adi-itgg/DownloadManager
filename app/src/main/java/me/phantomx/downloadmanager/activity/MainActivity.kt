package me.phantomx.downloadmanager.activity

import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.phantomx.downloadmanager.abstracs.ui.MainScreen
import me.phantomx.downloadmanager.data.FileDownload
import me.phantomx.downloadmanager.dialogs.DownloadFileDialog
import me.phantomx.downloadmanager.extensions.hasPermissions
import me.phantomx.downloadmanager.extensions.isRunningService
import me.phantomx.downloadmanager.listeners.AddJobListener
import me.phantomx.downloadmanager.sealed.DownloadStatus


class MainActivity : MainScreen(), AddJobListener {

    override fun onCreateActivity(savedInstanceState: Bundle?) = binding.run {

        VmPolicy.Builder().run {
            StrictMode.setVmPolicy(build())
            detectFileUriExposure()
        }

        val isServiceRunning = isRunningService()
        lifecycleScope.launchWhenCreated {
            if (!hasPermissions(permissions) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                requestPermissions(permissions.toTypedArray(), requestCodePermission)
            if (hasPermissions(permissions))
                createDirectory()

            for (fd in repository.getAll())
                downloadList.add(fd.apply {
                    if (!isServiceRunning && downloadStatus == DownloadStatus.Downloading) {
                        downloadStatus = DownloadStatus.Paused
                        repository.update(this)
                    }
                })

        }

        super.onCreateActivity(savedInstanceState)

        btnAddDialog.setOnClickListener {
            DownloadFileDialog(this@MainActivity, directory, okclient, this@MainActivity).show()
        }
    }

    override fun onNewJob(fileDownload: FileDownload) = fileDownload.run {
        try {
            downloadList.add(this)
            downloadService.runJob(this)
            lifecycleScope.launch {
                repository.put(this@run)
            }
            adapter.notifyItemInserted(downloadList.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}