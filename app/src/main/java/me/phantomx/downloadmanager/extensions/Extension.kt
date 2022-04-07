package me.phantomx.downloadmanager.extensions

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import me.phantomx.downloadmanager.services.DownloadService
import java.text.CharacterIterator
import java.text.StringCharacterIterator


fun Long.toReadable(): String {
    var bytes = this
    if (-1000 < bytes && bytes < 1000)
        return "$bytes B"
    val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
    while (bytes <= -999950 || bytes >= 999950) {
        bytes /= 1000
        ci.next()
    }
    return String.format("%.1f %cB", bytes / 1000.0, ci.current())
}

fun String.pTry(pattern: String) = try {
        split(pattern)[1]
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        this
    }

fun Activity.hasPermissions(permissions: List<String>): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        return Environment.isExternalStorageManager()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    return true
}

@Suppress("Deprecation")
fun Activity.isRunningService(): Boolean {
    (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).let {
        for (service in it.getRunningServices(Int.MAX_VALUE))
            if (DownloadService::class.java.name.equals(service.service.className))
                return true
    }
    return false
}