package me.phantomx.downloadmanager.abstracs.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import me.phantomx.downloadmanager.R
import me.phantomx.downloadmanager.activity.MainActivity
import me.phantomx.downloadmanager.data.FileDownload
import me.phantomx.downloadmanager.database.Repository
import me.phantomx.downloadmanager.extensions.toReadable
import me.phantomx.downloadmanager.modules.Modules
import okhttp3.OkHttpClient
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@AndroidEntryPoint
abstract class AppService: Service(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Job()

    @Inject
    lateinit var downloadDispatcher: ExecutorCoroutineDispatcher

    @Inject
    lateinit var okclient: OkHttpClient

    @Inject
    lateinit var repository: Repository

    val liveData: MutableLiveData<FileDownload> = MutableLiveData()
    val downloadJobs: MutableList<FileDownload> = ArrayList()
    var notifRunning = false
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService() = this@AppService
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    @Suppress("Deprecation")
    private fun createNotification(title: String, description: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val contentIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val chan = NotificationChannel(
                Modules.androidServiceChannelID,
                "Downloader", NotificationManager.IMPORTANCE_NONE
            )
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)

            val builder: Notification.Builder = Notification.Builder(this,
                Modules.androidServiceChannelID
            )
                .setContentTitle(title)
                .setContentText(description)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
            val notification: Notification = builder.build()
            startForeground(1956, notification)
        } else {
            val contentIntent = PendingIntent.getActivity(
                this,
                0, Intent(this, MainActivity::class.java), 0
            )
            val builder = NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(description)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true)
            val notification: Notification = builder.build()
            startForeground(1956, notification)
        }
    }

    fun notificationStatus() {
        if (downloadJobs.isNotEmpty()) {
            var allProgress = 0L
            var allSizeDownload = 0L
            var allSizeDownloaded = 0L
            var allSpeed = 0L
            for (job in downloadJobs) {
                allProgress += job.downloadProgress
                allSizeDownload += job.fileSize
                allSizeDownloaded += job.totalDownloaded
                allSpeed += job.downloadSpeed
            }
            allProgress /= downloadJobs.size
            createNotification(
                title = "Running ${downloadJobs.size} Job" + if (downloadJobs.size > 1) "s" else "",
                description = "${allProgress}% Downloading ${allSizeDownloaded.toReadable()} / ${allSizeDownload.toReadable()} | ${allSpeed.toReadable()}/s"
            )
        } else
            createNotification("DownloadService is running", "Idle")

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = run {
        launch { notificationStatus() }
        START_NOT_STICKY
    }

    override fun onBind(intent: Intent?) = binder

    override fun onDestroy() {
        if (coroutineContext.isActive)
            coroutineContext.cancel(CancellationException("Service is destroyed"))
        super.onDestroy()
    }
}