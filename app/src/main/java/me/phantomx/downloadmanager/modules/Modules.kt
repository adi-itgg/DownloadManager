package me.phantomx.downloadmanager.modules

import android.content.Context
import android.os.Environment
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import me.phantomx.downloadmanager.database.Database
import me.phantomx.downloadmanager.database.Repository
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Modules {

    const val androidServiceChannelID = "AnimeDesu"

    @Singleton
    @Provides
    fun provideOkHttpClient() = OkHttpClient.Builder().build()

    @Singleton
    @Provides
    fun provideDownloadDispatcher() = Executors.newCachedThreadPool {
        Thread {
            try {
                it.run()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
            }
        }.apply {
            priority = Thread.NORM_PRIORITY - 1
        }
    }.asCoroutineDispatcher()

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): Database = Room.databaseBuilder(context, Database::class.java, "file-download").build()

    @Singleton
    @Provides
    fun provideRepository(database: Database) = database.dao()

    @Singleton
    @Provides
    fun provideDirectory() = @Suppress("Deprecation") File(Environment.getExternalStorageDirectory(), "DownloadManager")

}