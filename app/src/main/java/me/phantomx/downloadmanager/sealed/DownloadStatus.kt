package me.phantomx.downloadmanager.sealed

sealed class DownloadStatus {

    object Paused: DownloadStatus() {
        override fun toString(): String {
            return "Paused"
        }
    }

    object Queue: DownloadStatus() {
        override fun toString(): String {
            return "Queue"
        }
    }

    object Downloading : DownloadStatus() {
        override fun toString(): String {
            return "Downloading"
        }
    }

    object Error : DownloadStatus() {
        override fun toString(): String {
            return "Error"
        }
    }

    object Completed : DownloadStatus() {
        override fun toString(): String {
            return "Completed"
        }
    }
}
