package me.phantomx.downloadmanager.abstracs.ui

import android.Manifest
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.phantomx.downloadmanager.abstracs.services.AppService
import me.phantomx.downloadmanager.adapter.DownloadAdapter
import me.phantomx.downloadmanager.data.FileDownload
import me.phantomx.downloadmanager.database.Repository
import me.phantomx.downloadmanager.databinding.ActivityMainBinding
import me.phantomx.downloadmanager.extensions.hasPermissions
import me.phantomx.downloadmanager.extensions.isRunningService
import me.phantomx.downloadmanager.services.DownloadService
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
abstract class MainScreen: AppCompatActivity(), Observer<FileDownload>, ServiceConnection {

    lateinit var binding: ActivityMainBinding
    lateinit var downloadService: DownloadService
    lateinit var adapter: DownloadAdapter

    private val permissions = listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val requestCodePermission = 22
    val downloadList: MutableList<FileDownload> = ArrayList()

    private var dialog: AlertDialog? = null

    @Inject
    lateinit var okclient: OkHttpClient
    @Inject
    lateinit var repository: Repository
    @Inject
    lateinit var directory: File

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                it.getSerializableExtra("job")?.let { serialize ->
                    (serialize as FileDownload).run {
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
            }
        }
    }

    private fun createDirectory() = directory.run {
        if (!exists())
            Log.d(javaClass.name, when {
                mkdir() -> "onCreate: Dir Created"
                mkdirs() ->  "onCreate: Dir Created"
                else ->  "onCreate: Failed create directory"
            })
    }

    open fun onCreateActivity(savedInstanceState: Bundle?) {
        if (!isRunningService()) {
            val myService = Intent(this, DownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(myService)
            else
                startService(myService)
            bindService(myService, this, Context.BIND_AUTO_CREATE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, IntentFilter("DM-Request"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onCreateActivity(savedInstanceState)
    }

    override fun onDestroy() {
        try {
            unbindService(this)
        } catch (e: IllegalArgumentException) {

        }
        super.onDestroy()
    }

    private fun checkPermissions(): Boolean {
        if (!hasPermissions(permissions) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions()
            return false
        } else
            createDirectory()
        return true
    }

    private fun requestPermissions() {
        if (dialog != null) return
        AlertDialog.Builder(this).run {
            setTitle("Need storage permision")
            setMessage("Please allow storage permission first!")
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).run {
                            data = Uri.parse("package:${packageName}")
                            activityResultLauncher.launch(this)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        activityResultLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                    return@setPositiveButton
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    requestPermissions(permissions.toTypedArray(), requestCodePermission)
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                if (!hasPermissions(permissions) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    finish()
            }
            setOnDismissListener {
                dialog = null
            }
            setCancelable(false)
            dialog = show()
        }
    }

    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK && !hasPermissions(permissions)) {
            Toast.makeText(this, "App doesn't work without storage permission!", Toast.LENGTH_SHORT)
                .show()
            lifecycleScope.launch {
                delay(5000)
                finish()
            }
        } else
            createDirectory()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permission: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permission, grantResults)
        if (requestCode != requestCodePermission) return
        if (!hasPermissions(permissions)) {
            Toast.makeText(this, "App doesn't work without storage permission!", Toast.LENGTH_SHORT)
                .show()
            lifecycleScope.launch {
                delay(5000)
                finish()
            }
        } else
            createDirectory()
    }

    override fun onChanged(t: FileDownload) {
        adapter.notifyItemChanged(downloadList.indexOf(t), Unit)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        downloadService = (service as AppService.LocalBinder).getService() as DownloadService
        downloadService.liveData.observe(this, this)
        lifecycleScope.launch(Main) {
            adapter = DownloadAdapter(
                downloadList,
                lifecycleScope.coroutineContext,
                downloadService,
                repository
            )
            binding.recycler.layoutManager = LinearLayoutManager(this@MainScreen)
            binding.recycler.adapter = adapter
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        downloadService.liveData.removeObserver(this)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            checkPermissions()
    }

}