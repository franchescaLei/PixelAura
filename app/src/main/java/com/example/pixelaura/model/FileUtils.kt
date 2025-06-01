package com.example.pixelaura.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

//simple fucntion for downloading the images from posts
fun downloadImage(context: Context, imageUrl: String) {
    val request = DownloadManager.Request(Uri.parse(imageUrl))
        .setTitle("Downloading image")
        .setDescription("Image is being downloaded...")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            "image_${System.currentTimeMillis()}.jpg"
        )
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request)

    Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
}
