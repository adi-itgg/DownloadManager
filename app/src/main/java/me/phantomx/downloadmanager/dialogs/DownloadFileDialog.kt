package me.phantomx.downloadmanager.dialogs

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import me.phantomx.downloadmanager.data.FileDownload
import me.phantomx.downloadmanager.databinding.DownloadFileDialogBinding
import me.phantomx.downloadmanager.extensions.pTry
import me.phantomx.downloadmanager.extensions.toReadable
import me.phantomx.downloadmanager.listeners.AddJobListener
import me.phantomx.downloadmanager.sealed.DownloadStatus
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.headersContentLength
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.coroutines.CoroutineContext

class DownloadFileDialog(context: Context, private val directory: File, private val client: OkHttpClient, private val listener: AddJobListener): AlertDialog(context), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Job()

    lateinit var binding: DownloadFileDialogBinding
    var jobGetFilename: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = DownloadFileDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setOnDismissListener {
            if (coroutineContext.isActive) coroutineContext.cancel(CancellationException("Dismiss!"))
        }

        initialize()
    }

    fun initialize() = binding.run {
        btnCancel.setOnClickListener { cancel() }

        url.doOnTextChanged { text, _, _, _ ->
            jobGetFilename?.cancel(null)
            jobGetFilename = launch(IO) {
                delay(400)
                try {
                    val url = text.toString().trim()

                    val response = client.newCall(Request.Builder().url(url).head().build()).execute()

                    val fileSize = response.headersContentLength()

                    val filename = response.headers["Content-Disposition"]?.let {
                        Uri.decode(it.pTry("filename").pTry("UTF-8").pTry("''"))
                    } ?: response.headers["Content-Type"]?.let {
                        "file.${it.split("/")[1]}"
                    } ?: FilenameUtils.getName(url)

                    val filen = filename.substring(0, filename.lastIndexOf(".")) + "_${ThreadLocalRandom.current().nextInt(100)}." + FilenameUtils.getExtension(filename)
                    val size = "Size: ${fileSize.toReadable()}"
                    withContext(Main) {
                        binding.filename.setText(filen)
                        binding.fileSize.text = size
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    e.printStackTrace()
                }
            }
        }

        btnDownload.setOnClickListener onClick@ {
            if (url.text.toString().isEmpty()) {
                Toast.makeText(context, "Url is empty!", Toast.LENGTH_SHORT).show()
                return@onClick
            }
            val filename = filename.text.toString()
            if (filename.length <= 5) {
                Toast.makeText(context, "Filename with extension is empty!", Toast.LENGTH_SHORT).show()
                return@onClick
            }
            val file = File(directory, filename)
            FileDownload(
                ThreadLocalRandom.current().nextLong(Long.MAX_VALUE),
                filename,
                DownloadStatus.Queue,
                0,
                binding.url.text.toString(),
                file.absolutePath,
                Calendar.getInstance().timeInMillis
            ).run {
                listener.onNewJob(this)
                Toast.makeText(context, "Download has been added", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

}