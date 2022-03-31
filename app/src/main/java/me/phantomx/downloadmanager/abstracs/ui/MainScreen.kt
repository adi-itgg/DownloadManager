package me.phantomx.downloadmanager.abstracs.ui

import android.Manifest
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers.Main
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

    val permissions = listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    val requestCodePermission = 22
    val downloadList: MutableList<FileDownload> = ArrayList()

    @Inject
    lateinit var okclient: OkHttpClient
    @Inject
    lateinit var repository: Repository
    @Inject
    lateinit var directory: File

    var broadcastReceiver = object : BroadcastReceiver() {
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

    fun createDirectory() = directory.run {
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
        unbindService(this)
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permission: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permission, grantResults)
        if (!hasPermissions(permissions)) {
            Toast.makeText(this, "App doesn't work without storage permission!", Toast.LENGTH_SHORT)
                .show()
            finish()
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
        binding.root.requestFocus()
    }

}