package me.phantomx.downloadmanager.abstracs.adapter

import android.content.*
import android.content.Context.CLIPBOARD_SERVICE
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.phantomx.downloadmanager.R
import me.phantomx.downloadmanager.data.FileDownload
import me.phantomx.downloadmanager.database.Repository
import me.phantomx.downloadmanager.databinding.RowBinding
import me.phantomx.downloadmanager.sealed.DownloadStatus
import me.phantomx.downloadmanager.services.DownloadService
import java.io.File
import kotlin.coroutines.CoroutineContext

abstract class RecyclerViewAdapter(
    private var data: MutableList<FileDownload>,
    private val coroutine: CoroutineContext,
    private val downloadService: DownloadService,
    private val repository: Repository
) :
    RecyclerView.Adapter<RecyclerViewAdapter.Holder>(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = coroutine

    inner class Holder(val binding: RowBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.run {
                more.setOnClickListener {
                    val data = adapterPosition.data()
                    when (data.downloadStatus) {
                        DownloadStatus.Paused -> downloadService.runJob(data)
                        DownloadStatus.Downloading -> {
                            data.downloadJob?.cancel(CancellationException("Pause")) ?: run {
                                data.downloadStatus = DownloadStatus.Paused
                            }
                        }
                        else -> it.showContextMenuItem(adapterPosition)
                    }
                    notifyItemChanged(adapterPosition)
                }
                root.setOnClickListener {
                    it.context.openFile(adapterPosition.data().saveLocation)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = parent.run {
        Holder(RowBinding.inflate(LayoutInflater.from(context), this, false))
    }

    override fun getItemCount() = data.size

    fun Int.data() = data[this]

    fun View.showContextMenuItem(position: Int) =
        PopupMenu(context, this).run {
            inflate(R.menu.context_item)
            setOnMenuItemClickListener { menu ->
                position.data().run fd@{
                    when (menu?.itemId) {
                        R.id.open -> context.openFile(saveLocation)
                        R.id.openFolder ->
                            context.openFile(
                                File(saveLocation).parent
                                    ?: saveLocation.also {
                                        it.substring(0, it.lastIndexOf(File.separator))
                                    })
                        R.id.redownload -> {
                            totalDownloaded = 0L
                            downloadProgress = 0
                            etaDownloadMs = 0L
                            remainingTimeMs = 0L
                            downloadService.runJob(this)
                            true
                        }
                        R.id.remove -> {
                            launch {
                                repository.delete(this@fd)
                                data.removeAt(data.indexOf(this@fd))
                                withContext(Main) {
                                    notifyItemRemoved(position)
                                }
                            }
                            true
                        }
                        R.id.delete -> {
                            if (File(saveLocation).delete())
                                Toast.makeText(
                                    context,
                                    "File has been deleted!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            true
                        }
                        R.id.copydownloadlink -> {
                            (context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                                ClipData.newRawUri("Download Link", Uri.parse(url))
                            )
                            true
                        }
                        else -> false
                    }
                }
            }
            show()
        }

    private fun getMimeType(url: String) =
        MimeTypeMap.getFileExtensionFromUrl(url).run {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(this) ?: "*/*"
        }

    private fun Context.openFile(filePath: String): Boolean {
        try {
            if (File(filePath).isDirectory)
                return Intent(Intent.ACTION_GET_CONTENT).run {
                    setDataAndType(Uri.parse(filePath + File.separator), "*/*")
                    addCategory("android.intent.category.DEFAULT")
                    val chooser = Intent.createChooser(this, "Open Folder")
                    startActivity(chooser)
                    true
                }

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                File(filePath)
            )

            val mime: String = getMimeType(uri.toString())

            return Intent(Intent.ACTION_VIEW).run {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(uri, mime)
                try {
                    startActivity(this)
                    true
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(
                        this@openFile,
                        "No one application can open this file",
                        Toast.LENGTH_SHORT
                    ).show()
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

}